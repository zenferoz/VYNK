#!/usr/bin/env python3
"""
VYNK Local Semantic Inference Service (Phase 4)
Provides local, free semantic similarity scoring using scikit-learn TF-IDF & Cosine Similarity.
Runs locally on 127.0.0.1:8001 by default.
"""

import json
import logging
import os
import sys
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import List

try:
    from sklearn.feature_extraction.text import TfidfVectorizer
    from sklearn.metrics.pairwise import cosine_similarity
except ImportError:
    print("Error: scikit-learn is required. Run 'pip install scikit-learn'.", file=sys.stderr)
    sys.exit(1)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
logger = logging.getLogger("vynk-semantic-inference")

HOST = os.environ.get("VYNK_INFERENCE_HOST", "127.0.0.1")
PORT = int(os.environ.get("VYNK_INFERENCE_PORT", 8001))
MAX_TEXT_LEN = 2000
MAX_CANDIDATES = 50


class SemanticInferenceHandler(BaseHTTPRequestHandler):

    def log_message(self, format, *args):
        # Override to use standard logger
        logger.info("%s - %s", self.address_string(), format % args)

    def _send_json(self, status_code: int, data: dict):
        response_body = json.dumps(data).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(response_body)))
        self.end_headers()
        self.wfile.write(response_body)

    def do_GET(self):
        if self.path == "/health":
            self._send_json(200, {
                "status": "healthy",
                "service": "vynk-semantic-inference",
                "model": "sklearn-tfidf-cosine",
                "max_text_len": MAX_TEXT_LEN,
                "max_candidates": MAX_CANDIDATES
            })
        else:
            self._send_json(404, {"error": "Not Found", "path": self.path})

    def do_POST(self):
        if self.path != "/semantic-similarity":
            self._send_json(404, {"error": "Not Found", "path": self.path})
            return

        content_length = int(self.headers.get("Content-Length", 0))
        if content_length == 0:
            self._send_json(400, {"error": "Empty request body"})
            return

        try:
            body = self.rfile.read(content_length).decode("utf-8")
            payload = json.loads(body)
        except Exception as e:
            logger.error("Failed to parse request JSON: %s", e)
            self._send_json(400, {"error": "Invalid JSON payload"})
            return

        source_text = payload.get("source_text", "").strip()
        candidate_texts = payload.get("candidate_texts", [])

        if not isinstance(candidate_texts, list):
            self._send_json(400, {"error": "candidate_texts must be a list"})
            return

        if len(candidate_texts) > MAX_CANDIDATES:
            self._send_json(400, {"error": f"Too many candidates. Maximum allowed is {MAX_CANDIDATES}."})
            return

        if not source_text or not candidate_texts:
            self._send_json(200, {
                "status": "ok",
                "similarities": [0.0] * len(candidate_texts)
            })
            return

        # Sanitize text lengths
        clean_source = source_text[:MAX_TEXT_LEN]
        clean_candidates = [str(c)[:MAX_TEXT_LEN].strip() for c in candidate_texts]

        try:
            corpus = [clean_source] + clean_candidates
            vectorizer = TfidfVectorizer(
                ngram_range=(1, 2),
                stop_words="english",
                token_pattern=r"(?u)\b\w+\b"
            )
            tfidf_matrix = vectorizer.fit_transform(corpus)
            source_vec = tfidf_matrix[0:1]
            candidate_vecs = tfidf_matrix[1:]

            scores = cosine_similarity(source_vec, candidate_vecs)[0]
            similarities = [float(round(max(0.0, min(1.0, s)), 4)) for s in scores]

            self._send_json(200, {
                "status": "ok",
                "similarities": similarities
            })
        except Exception as e:
            logger.error("Error computing semantic similarity: %s", e)
            self._send_json(500, {"error": "Inference computation error", "details": str(e)})


def run_server(host=HOST, port=PORT):
    server = HTTPServer((host, port), SemanticInferenceHandler)
    logger.info("VYNK Semantic Inference Service started at http://%s:%d", host, port)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        logger.info("Shutting down inference server...")
        server.server_close()


if __name__ == "__main__":
    run_server()

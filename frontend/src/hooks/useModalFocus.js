import { useEffect, useRef, useCallback } from 'react';

const FOCUSABLE_SELECTOR =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * Custom hook for accessible modal dialog keyboard and focus management.
 *
 * Implements WAI-ARIA modal dialog focus requirements:
 * 1. Saves triggering active element and restores focus upon closing.
 * 2. Focuses initial interactive element inside the modal on open.
 * 3. Traps focus within modal boundaries on Tab and Shift+Tab.
 * 4. Dismisses dialog on Escape key (prevented during active submission).
 * 5. Handles backdrop clicks safely while preventing inner click bubbling.
 *
 * @param {{
 *   isOpen?: boolean,
 *   onClose?: () => void,
 *   submitting?: boolean,
 *   initialFocusRef?: React.RefObject<HTMLElement | null>,
 *   initialFocusSelector?: string
 * }} options
 */
export function useModalFocus({
  isOpen = true,
  onClose,
  submitting = false,
  initialFocusRef,
  initialFocusSelector,
} = {}) {
  const modalRef = useRef(null);
  const previousActiveElementRef = useRef(null);

  // 1. Initial focus placement and focus restoration
  useEffect(() => {
    if (!isOpen) return;

    // Capture triggering element
    previousActiveElementRef.current = document.activeElement;

    const modalElement = modalRef.current;
    if (!modalElement) return;

    const focusTimer = requestAnimationFrame(() => {
      // Prioritize explicit ref
      if (initialFocusRef && initialFocusRef.current && !initialFocusRef.current.disabled) {
        initialFocusRef.current.focus();
        return;
      }

      // Prioritize selector
      if (initialFocusSelector) {
        const target = modalElement.querySelector(initialFocusSelector);
        if (target && !target.disabled && typeof target.focus === 'function') {
          target.focus();
          return;
        }
      }

      // Fallback to first focusable element
      const focusables = modalElement.querySelectorAll(FOCUSABLE_SELECTOR);
      if (focusables.length > 0) {
        focusables[0].focus();
      } else {
        modalElement.focus();
      }
    });

    return () => {
      cancelAnimationFrame(focusTimer);
      const prevElement = previousActiveElementRef.current;
      if (
        prevElement &&
        typeof prevElement.focus === 'function' &&
        document.body.contains(prevElement)
      ) {
        prevElement.focus();
      }
    };
  }, [isOpen, initialFocusRef, initialFocusSelector]);

  // 2. Keyboard event handling: Escape dismissal & Tab focus containment
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      // Escape handling
      if (e.key === 'Escape') {
        if (!submitting && onClose) {
          e.preventDefault();
          e.stopPropagation();
          onClose();
        }
        return;
      }

      // Tab focus trap
      if (e.key === 'Tab') {
        const modalElement = modalRef.current;
        if (!modalElement) return;

        const focusables = Array.from(modalElement.querySelectorAll(FOCUSABLE_SELECTOR));
        if (focusables.length === 0) {
          e.preventDefault();
          return;
        }

        const firstElement = focusables[0];
        const lastElement = focusables[focusables.length - 1];

        if (e.shiftKey) {
          // Shift + Tab: wrap to last if on first or outside
          if (document.activeElement === firstElement || !modalElement.contains(document.activeElement)) {
            e.preventDefault();
            lastElement.focus();
          }
        } else {
          // Tab: wrap to first if on last or outside
          if (document.activeElement === lastElement || !modalElement.contains(document.activeElement)) {
            e.preventDefault();
            firstElement.focus();
          }
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown, true);
    return () => {
      document.removeEventListener('keydown', handleKeyDown, true);
    };
  }, [isOpen, onClose, submitting]);

  // 3. Safe backdrop click handler
  const handleOverlayClick = useCallback(
    (e) => {
      if (e.target === e.currentTarget && !submitting && onClose) {
        onClose();
      }
    },
    [onClose, submitting]
  );

  return {
    modalRef,
    handleOverlayClick,
  };
}

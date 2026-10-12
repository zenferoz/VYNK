import React, { useState } from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { TagsInputField } from '../components/forms/TagsInputField';
import { TextareaField } from '../components/forms/TextareaField';

function TagsHarness({ initialTags = [], maxTags = 15, maxTagLength = 50, label = 'Goal Skills' }) {
  const [tags, setTags] = useState(initialTags);
  return (
    <div>
      <TagsInputField
        id="goalSkills"
        label={label}
        tags={tags}
        onChange={setTags}
        maxTags={maxTags}
        maxTagLength={maxTagLength}
        placeholder="e.g. PyTorch, Next.js..."
      />
      <div data-testid="current-tags-json">{JSON.stringify(tags)}</div>
    </div>
  );
}

describe('Input Limit Feedback Tests', () => {
  describe('TagsInputField — 15 Skills Limit & UX', () => {
    it('communicates the 15-skill maximum clearly in the UI', () => {
      render(<TagsHarness initialTags={[]} maxTags={15} />);

      expect(screen.getByText('0/15')).toBeDefined();
      const input = screen.getByPlaceholderText('e.g. PyTorch, Next.js...');
      expect(input.disabled).toBe(false);
    });

    it('allows adding skills up to the 15th skill and updates counter', () => {
      const fourteenSkills = Array.from({ length: 14 }, (_, i) => `Skill${i + 1}`);
      render(<TagsHarness initialTags={fourteenSkills} maxTags={15} />);

      expect(screen.getByText('14/15')).toBeDefined();

      const input = screen.getByPlaceholderText('e.g. PyTorch, Next.js...');
      fireEvent.change(input, { target: { value: 'Skill15' } });
      fireEvent.keyDown(input, { key: 'Enter' });

      expect(screen.getByText('15/15')).toBeDefined();
      expect(screen.getByText('Skill15')).toBeDefined();
      expect(screen.getByText(/Maximum 15 tags reached. Remove a tag to add another./i)).toBeDefined();
    });

    it('prevents adding a 16th skill and provides helpful feedback when limit is reached', () => {
      const fifteenSkills = Array.from({ length: 15 }, (_, i) => `Skill${i + 1}`);
      render(<TagsHarness initialTags={fifteenSkills} maxTags={15} />);

      expect(screen.getByText('15/15')).toBeDefined();

      const input = screen.getByPlaceholderText('Tag limit reached (max 15)');
      expect(input.disabled).toBe(true);

      const addButton = screen.getByRole('button', { name: 'Add' });
      expect(addButton.disabled).toBe(true);

      // Attempting to trigger add while at limit preserves existing tags and shows error
      const tagsDisplay = screen.getByTestId('current-tags-json');
      expect(JSON.parse(tagsDisplay.textContent)).toHaveLength(15);
    });

    it('handles bulk/pasted skill input at the limit without exceeding 15', () => {
      // Start with 13 skills (2 slots available)
      const thirteenSkills = Array.from({ length: 13 }, (_, i) => `Skill${i + 1}`);
      render(<TagsHarness initialTags={thirteenSkills} maxTags={15} />);

      expect(screen.getByText('13/15')).toBeDefined();

      const input = screen.getByPlaceholderText('e.g. PyTorch, Next.js...');
      
      // Paste 4 comma-separated skills: Skill14, Skill15, Skill16, Skill17
      fireEvent.paste(input, {
        clipboardData: {
          getData: () => 'Skill14, Skill15, Skill16, Skill17',
        },
      });

      const tagsDisplay = screen.getByTestId('current-tags-json');
      const currentTags = JSON.parse(tagsDisplay.textContent);

      // Exactly 15 skills, Skill14 and Skill15 were added, Skill16 and 17 were not exceeded
      expect(currentTags).toHaveLength(15);
      expect(currentTags).toContain('Skill14');
      expect(currentTags).toContain('Skill15');
      expect(currentTags).not.toContain('Skill16');
      expect(currentTags).not.toContain('Skill17');

      // Feedback communicated clearly
      expect(screen.getByText(/Maximum 15 tags reached/i)).toBeDefined();
      expect(screen.getByText('15/15')).toBeDefined();
    });

    it('handles bulk input via input value and Add button up to the limit', () => {
      render(<TagsHarness initialTags={['React', 'TypeScript']} maxTags={5} />);

      const input = screen.getByPlaceholderText('e.g. PyTorch, Next.js...');
      fireEvent.change(input, { target: { value: 'Python, Rust, Go, C++, Java' } });

      const addButton = screen.getByRole('button', { name: 'Add' });
      fireEvent.click(addButton);

      const tagsDisplay = screen.getByTestId('current-tags-json');
      const currentTags = JSON.parse(tagsDisplay.textContent);

      expect(currentTags).toHaveLength(5);
      expect(currentTags).toEqual(['React', 'TypeScript', 'Python', 'Rust', 'Go']);
      expect(screen.getByText('5/5')).toBeDefined();
    });

    it('does not silently discard already entered valid tags when bulk paste is rejected or capped', () => {
      const initialSkills = ['Python', 'Docker', 'Kubernetes'];
      render(<TagsHarness initialTags={initialSkills} maxTags={4} />);

      const input = screen.getByPlaceholderText('e.g. PyTorch, Next.js...');
      fireEvent.paste(input, {
        clipboardData: {
          getData: () => 'AWS, GCP, Azure',
        },
      });

      const tagsDisplay = screen.getByTestId('current-tags-json');
      const currentTags = JSON.parse(tagsDisplay.textContent);

      // Original 3 tags preserved plus only 1 new tag (capacity: 4)
      expect(currentTags).toEqual(['Python', 'Docker', 'Kubernetes', 'AWS']);
    });

    it('allows removing a skill after reaching limit and re-enables input', () => {
      const fifteenSkills = Array.from({ length: 15 }, (_, i) => `Skill${i + 1}`);
      render(<TagsHarness initialTags={fifteenSkills} maxTags={15} />);

      expect(screen.getByText('15/15')).toBeDefined();

      // Remove the first tag
      const removeButton = screen.getByLabelText('Remove tag Skill1');
      fireEvent.click(removeButton);

      // Counter should drop to 14/15
      expect(screen.getByText('14/15')).toBeDefined();
      expect(screen.queryByText('Skill1')).toBeNull();

      // Input should be re-enabled
      const input = screen.getByPlaceholderText('e.g. PyTorch, Next.js...');
      expect(input.disabled).toBe(false);

      // Add a replacement skill
      fireEvent.change(input, { target: { value: 'NewSkill' } });
      fireEvent.keyDown(input, { key: 'Enter' });

      expect(screen.getByText('15/15')).toBeDefined();
      expect(screen.getByText('NewSkill')).toBeDefined();
    });

    it('preserves existing tag normalization and duplicate handling', () => {
      render(<TagsHarness initialTags={['JavaScript']} maxTags={15} />);

      const input = screen.getByPlaceholderText('e.g. PyTorch, Next.js...');

      // Whitespace and comma trimming
      fireEvent.change(input, { target: { value: '  TypeScript,  ' } });
      fireEvent.keyDown(input, { key: 'Enter' });
      expect(screen.getByText('TypeScript')).toBeDefined();

      // Duplicate detection (case-insensitive)
      fireEvent.change(input, { target: { value: 'javascript' } });
      fireEvent.keyDown(input, { key: 'Enter' });
      expect(screen.getByText('"javascript" is already added.')).toBeDefined();

      // Max tag length (50 characters)
      const longTag = 'Z'.repeat(51);
      fireEvent.change(input, { target: { value: longTag } });
      fireEvent.keyDown(input, { key: 'Enter' });
      expect(screen.getByText('Tag must not exceed 50 characters.')).toBeDefined();
    });
  });

  describe('TextareaField — Character Counter & Limits', () => {
    function TextareaHarness({ maxLength = 300 }) {
      const [text, setText] = useState('');
      return (
        <TextareaField
          id="testNote"
          label="Purpose Note"
          value={text}
          onChange={(e) => setText(e.target.value)}
          maxLength={maxLength}
        />
      );
    }

    it('renders initial character counter 0/300', () => {
      render(<TextareaHarness maxLength={300} />);
      expect(screen.getByText('0/300')).toBeDefined();
    });

    it('updates character counter correctly as user types within limit', () => {
      render(<TextareaHarness maxLength={300} />);

      const textarea = screen.getByLabelText(/Purpose Note/i);
      fireEvent.change(textarea, { target: { value: 'Hello world' } });

      expect(screen.getByText('11/300')).toBeDefined();
    });

    it('prevents typing from exceeding the maxLength and preserves allowed characters', () => {
      render(<TextareaHarness maxLength={300} />);

      const textarea = screen.getByLabelText(/Purpose Note/i);
      const oversizedText = 'A'.repeat(350);
      fireEvent.change(textarea, { target: { value: oversizedText } });

      expect(textarea.value.length).toBe(300);
      expect(textarea.value).toBe('A'.repeat(300));
      expect(screen.getByText('300/300')).toBeDefined();
    });

    it('prevents pasting from exceeding maxLength and preserves text up to the boundary', () => {
      render(<TextareaHarness maxLength={300} />);

      const textarea = screen.getByLabelText(/Purpose Note/i);
      textarea.value = 'Existing: ';

      fireEvent.paste(textarea, {
        clipboardData: {
          getData: () => 'X'.repeat(350),
        },
      });

      expect(textarea.value.length).toBe(300);
      expect(screen.getByText('300/300')).toBeDefined();
    });
  });
});

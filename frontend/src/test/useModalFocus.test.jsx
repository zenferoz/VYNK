import React, { useRef } from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { useModalFocus } from '../hooks/useModalFocus';

function TestModalHarness({
  isOpen = true,
  onClose,
  submitting = false,
  initialFocusSelector,
  customInitialFocusRef = false,
  emptyContent = false,
  extraRerenderProp,
}) {
  const customRef = useRef(null);
  const { modalRef, handleOverlayClick } = useModalFocus({
    isOpen,
    onClose,
    submitting,
    initialFocusRef: customInitialFocusRef ? customRef : undefined,
    initialFocusSelector,
  });

  if (!isOpen) return null;

  return (
    <div data-testid="overlay" onClick={handleOverlayClick}>
      <div data-testid="modal" ref={modalRef} tabIndex={-1}>
        <span data-testid="rerender-indicator">{extraRerenderProp}</span>
        {emptyContent ? (
          <div>No interactive controls here</div>
        ) : (
          <>
            <button data-testid="modal-btn-1">First Button</button>
            <input data-testid="modal-input" placeholder="Type here" />
            <button data-testid="modal-btn-2" ref={customInitialFocusRef ? customRef : null}>
              Second Button
            </button>
          </>
        )}
      </div>
    </div>
  );
}

describe('useModalFocus Hook', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('initial focus enters the modal on the first focusable element by default', () => {
    const trigger = document.createElement('button');
    trigger.textContent = 'Open Modal';
    document.body.appendChild(trigger);
    trigger.focus();
    expect(document.activeElement).toBe(trigger);

    render(<TestModalHarness isOpen={true} />);

    // Advance frame
    act(() => {
      vi.advanceTimersByTime(20);
    });

    const firstBtn = screen.getByTestId('modal-btn-1');
    expect(document.activeElement).toBe(firstBtn);

    document.body.removeChild(trigger);
  });

  it('initial focus prioritizes initialFocusSelector when provided', () => {
    render(<TestModalHarness isOpen={true} initialFocusSelector="[data-testid='modal-input']" />);

    act(() => {
      vi.advanceTimersByTime(20);
    });

    const input = screen.getByTestId('modal-input');
    expect(document.activeElement).toBe(input);
  });

  it('initial focus prioritizes initialFocusRef when provided', () => {
    render(<TestModalHarness isOpen={true} customInitialFocusRef={true} />);

    act(() => {
      vi.advanceTimersByTime(20);
    });

    const secondBtn = screen.getByTestId('modal-btn-2');
    expect(document.activeElement).toBe(secondBtn);
  });

  it('falls back to focusing modalElement if no focusable children exist', () => {
    render(<TestModalHarness isOpen={true} emptyContent={true} />);

    act(() => {
      vi.advanceTimersByTime(20);
    });

    const modal = screen.getByTestId('modal');
    expect(document.activeElement).toBe(modal);
  });

  it('calls onClose when Escape key is pressed while not submitting', () => {
    const handleClose = vi.fn();
    render(<TestModalHarness isOpen={true} onClose={handleClose} submitting={false} />);

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('does NOT call onClose when Escape key is pressed during submission', () => {
    const handleClose = vi.fn();
    render(<TestModalHarness isOpen={true} onClose={handleClose} submitting={true} />);

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(handleClose).not.toHaveBeenCalled();
  });

  it('traps focus: Tab wraps from the last focusable element to the first', () => {
    render(<TestModalHarness isOpen={true} />);
    const firstBtn = screen.getByTestId('modal-btn-1');
    const lastBtn = screen.getByTestId('modal-btn-2');

    lastBtn.focus();
    expect(document.activeElement).toBe(lastBtn);

    fireEvent.keyDown(document, { key: 'Tab', shiftKey: false });
    expect(document.activeElement).toBe(firstBtn);
  });

  it('traps focus: Shift+Tab wraps from the first focusable element to the last', () => {
    render(<TestModalHarness isOpen={true} />);
    const firstBtn = screen.getByTestId('modal-btn-1');
    const lastBtn = screen.getByTestId('modal-btn-2');

    firstBtn.focus();
    expect(document.activeElement).toBe(firstBtn);

    fireEvent.keyDown(document, { key: 'Tab', shiftKey: true });
    expect(document.activeElement).toBe(lastBtn);
  });

  it('recovers focus when Tab is pressed while focus is outside the modal', () => {
    const outsideBtn = document.createElement('button');
    outsideBtn.textContent = 'Outside';
    document.body.appendChild(outsideBtn);

    render(<TestModalHarness isOpen={true} />);
    const firstBtn = screen.getByTestId('modal-btn-1');
    const lastBtn = screen.getByTestId('modal-btn-2');

    // Focus is outside modal
    outsideBtn.focus();
    expect(document.activeElement).toBe(outsideBtn);

    // Forward Tab brings focus back to first element in modal
    fireEvent.keyDown(document, { key: 'Tab', shiftKey: false });
    expect(document.activeElement).toBe(firstBtn);

    // Focus outside again
    outsideBtn.focus();
    expect(document.activeElement).toBe(outsideBtn);

    // Shift+Tab brings focus to last element in modal
    fireEvent.keyDown(document, { key: 'Tab', shiftKey: true });
    expect(document.activeElement).toBe(lastBtn);

    document.body.removeChild(outsideBtn);
  });

  it('prevents default Tab behavior if modal has zero focusable elements', () => {
    render(<TestModalHarness isOpen={true} emptyContent={true} />);

    const tabEvent = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true });
    const preventDefaultSpy = vi.spyOn(tabEvent, 'preventDefault');

    document.dispatchEvent(tabEvent);
    expect(preventDefaultSpy).toHaveBeenCalled();
  });

  it('restores focus to the triggering element upon unmount', () => {
    const trigger = document.createElement('button');
    trigger.setAttribute('data-testid', 'trigger-button');
    document.body.appendChild(trigger);
    trigger.focus();
    expect(document.activeElement).toBe(trigger);

    const { unmount } = render(<TestModalHarness isOpen={true} />);

    act(() => {
      vi.advanceTimersByTime(20);
    });

    expect(document.activeElement).toBe(screen.getByTestId('modal-btn-1'));

    unmount();

    expect(document.activeElement).toBe(trigger);
    document.body.removeChild(trigger);
  });

  it('does NOT restore focus to the trigger on ordinary re-renders while the modal remains open', () => {
    const trigger = document.createElement('button');
    document.body.appendChild(trigger);
    trigger.focus();

    const { rerender } = render(
      <TestModalHarness isOpen={true} extraRerenderProp="initial" />
    );

    act(() => {
      vi.advanceTimersByTime(20);
    });

    // Move focus manually to the input inside the modal
    const input = screen.getByTestId('modal-input');
    input.focus();
    expect(document.activeElement).toBe(input);

    // Re-render with updated prop (e.g., submitting state or other data change)
    rerender(<TestModalHarness isOpen={true} extraRerenderProp="updated" submitting={true} />);

    // Focus must remain on the input inside the modal and NOT snap back to trigger
    expect(document.activeElement).toBe(input);
    expect(document.activeElement).not.toBe(trigger);

    document.body.removeChild(trigger);
  });

  it('cleans up event listeners and scheduled animation frame on unmount', () => {
    const cancelAnimationFrameSpy = vi.spyOn(window, 'cancelAnimationFrame');
    const removeEventListenerSpy = vi.spyOn(document, 'removeEventListener');
    const handleClose = vi.fn();

    const { unmount } = render(<TestModalHarness isOpen={true} onClose={handleClose} />);

    expect(removeEventListenerSpy).not.toHaveBeenCalledWith('keydown', expect.any(Function), true);

    unmount();

    expect(cancelAnimationFrameSpy).toHaveBeenCalled();
    expect(removeEventListenerSpy).toHaveBeenCalledWith('keydown', expect.any(Function), true);

    // Further Escape presses must have no effect
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(handleClose).not.toHaveBeenCalled();
  });

  it('handleOverlayClick triggers onClose when clicking backdrop directly, but not during submission', () => {
    const handleClose = vi.fn();
    const { rerender } = render(
      <TestModalHarness isOpen={true} onClose={handleClose} submitting={false} />
    );

    const overlay = screen.getByTestId('overlay');
    fireEvent.click(overlay);
    expect(handleClose).toHaveBeenCalledTimes(1);

    // When submitting, overlay click is prevented
    rerender(<TestModalHarness isOpen={true} onClose={handleClose} submitting={true} />);
    fireEvent.click(overlay);
    expect(handleClose).toHaveBeenCalledTimes(1); // Still 1, not called again
  });
});

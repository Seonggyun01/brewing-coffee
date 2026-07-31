document.querySelectorAll('[data-form-wizard]').forEach((form) => {
    const steps = Array.from(form.querySelectorAll('[data-wizard-step]'));
    const stepButtons = Array.from(form.querySelectorAll('[data-wizard-step-target]'));
    const prevButton = form.querySelector('[data-wizard-prev]');
    const nextButton = form.querySelector('[data-wizard-next]');
    const submitButton = form.querySelector('[data-wizard-submit]');
    let activeIndex = 0;

    if (steps.length <= 1) {
        return;
    }

    function setActiveStep(index, shouldScroll = true) {
        activeIndex = Math.min(Math.max(index, 0), steps.length - 1);

        steps.forEach((step, stepIndex) => {
            step.hidden = stepIndex !== activeIndex;
        });

        stepButtons.forEach((button, buttonIndex) => {
            const isActive = buttonIndex === activeIndex;
            button.classList.toggle('is-active', isActive);
            button.setAttribute('aria-current', isActive ? 'step' : 'false');
        });

        if (prevButton) {
            prevButton.disabled = activeIndex === 0;
        }
        if (nextButton) {
            nextButton.hidden = activeIndex === steps.length - 1;
        }
        if (submitButton) {
            submitButton.hidden = activeIndex !== steps.length - 1;
        }

        if (shouldScroll) {
            form.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }

    stepButtons.forEach((button, index) => {
        button.addEventListener('click', () => setActiveStep(index));
    });
    prevButton?.addEventListener('click', () => setActiveStep(activeIndex - 1));
    nextButton?.addEventListener('click', () => setActiveStep(activeIndex + 1));

    setActiveStep(0, false);
});

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

    function findInvalidControl() {
        return Array.from(form.querySelectorAll('input:not([type="hidden"]), select, textarea'))
            .find((control) => !control.disabled && typeof control.checkValidity === 'function' && !control.checkValidity());
    }

    function showControlStep(control) {
        const controlStep = control.closest('[data-wizard-step]');
        const controlStepIndex = steps.indexOf(controlStep);

        if (controlStepIndex !== -1) {
            setActiveStep(controlStepIndex);
        }

        window.requestAnimationFrame(() => {
            if (typeof control.reportValidity === 'function') {
                control.reportValidity();
            }
            if (typeof control.focus === 'function') {
                control.focus({ preventScroll: true });
            }
        });
    }

    function setActiveStep(index, shouldScroll = true) {
        activeIndex = Math.min(Math.max(index, 0), steps.length - 1);

        form.style.setProperty('--wizard-progress', `${((activeIndex + 1) / steps.length) * 100}%`);
        form.dataset.wizardCurrentStep = String(activeIndex + 1);
        form.dataset.wizardTotalSteps = String(steps.length);

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
    nextButton?.addEventListener('click', () => {
        const invalidControl = findInvalidControl();

        if (invalidControl) {
            const invalidStep = invalidControl.closest('[data-wizard-step]');
            const invalidStepIndex = steps.indexOf(invalidStep);

            if (invalidStepIndex === activeIndex) {
                showControlStep(invalidControl);
                return;
            }
        }

        setActiveStep(activeIndex + 1);
    });
    submitButton?.addEventListener('click', (event) => {
        const invalidControl = findInvalidControl();

        if (!invalidControl) {
            return;
        }

        event.preventDefault();
        showControlStep(invalidControl);
    });
    form.addEventListener('invalid', (event) => {
        if (event.target instanceof HTMLElement) {
            showControlStep(event.target);
        }
    }, true);

    setActiveStep(0, false);
});

function showModelWaitOverlay(title, description) {
    const overlay = document.querySelector('[data-model-wait-overlay]');
    const titleElement = overlay?.querySelector('[data-model-wait-title]');
    const descriptionElement = overlay?.querySelector('[data-model-wait-description]');

    if (!overlay) {
        return;
    }

    if (titleElement) {
        titleElement.textContent = title;
    }
    if (descriptionElement) {
        descriptionElement.textContent = description;
    }

    overlay.hidden = false;
    document.body.classList.add('is-model-waiting');
}

document.querySelectorAll('[data-card-extraction-form]').forEach((form) => {
    const submitButton = form.querySelector('button[type="submit"]');

    form.addEventListener('submit', () => {
        form.setAttribute('aria-busy', 'true');
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.dataset.originalText = submitButton.textContent || '';
            submitButton.textContent = '분석 중...';
        }
        showModelWaitOverlay('응답 대기 중입니다', '이미지를 읽고 원두 정보를 정리하고 있어요.');
    });
});

document.querySelectorAll('[data-model-save-form]').forEach((form) => {
    const submitButton = form.querySelector('[data-wizard-submit], button[type="submit"]');
    const customFlavorNotesInput = form.querySelector('[name="customFlavorNotesText"]');
    const waitTitle = form.dataset.modelSaveTitle || '저장중입니다';
    const waitDescription = form.dataset.modelSaveDescription || '새 향미 노트의 색상을 정리하고 저장하고 있어요.';

    form.addEventListener('submit', () => {
        const canValidate = typeof form.checkValidity === 'function';
        const shouldWaitForModel = customFlavorNotesInput?.value?.trim();

        if ((canValidate && !form.checkValidity()) || !shouldWaitForModel) {
            return;
        }

        form.setAttribute('aria-busy', 'true');
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.dataset.originalText = submitButton.textContent || '';
            submitButton.textContent = '저장 중...';
        }
        showModelWaitOverlay(waitTitle, waitDescription);
    });
});

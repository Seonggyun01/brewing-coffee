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

const extractionPhaseOrder = ['OCR', 'LLM', 'PARSING'];

function updateExtractionProgress(phase) {
    const overlay = document.querySelector('[data-model-wait-overlay]');
    const progress = overlay?.querySelector('[data-extraction-progress]');
    const titleElement = overlay?.querySelector('[data-model-wait-title]');
    const descriptionElement = overlay?.querySelector('[data-model-wait-description]');
    const stages = progress?.querySelectorAll('[data-extraction-phase]') || [];
    const currentIndex = phase === 'COMPLETED'
        ? extractionPhaseOrder.length
        : extractionPhaseOrder.indexOf(phase);

    stages.forEach((stage, index) => {
        stage.classList.toggle('is-complete', index < currentIndex || phase === 'COMPLETED');
        stage.classList.toggle('is-active', index === currentIndex);
        if (index === currentIndex) {
            stage.setAttribute('aria-current', 'step');
        } else {
            stage.removeAttribute('aria-current');
        }
    });

    if (phase === 'COMPLETED') {
        if (titleElement) {
            titleElement.textContent = '자동 입력을 마무리하고 있어요';
        }
        if (descriptionElement) {
            descriptionElement.textContent = '정리한 결과를 등록 화면에 반영하고 있어요.';
        }
    }
}

function showModelWaitOverlay(title, description, showExtractionProgress = false) {
    const overlay = document.querySelector('[data-model-wait-overlay]');
    const titleElement = overlay?.querySelector('[data-model-wait-title]');
    const descriptionElement = overlay?.querySelector('[data-model-wait-description]');
    const progressElement = overlay?.querySelector('[data-extraction-progress]');

    if (!overlay) {
        return;
    }

    if (titleElement) {
        titleElement.textContent = title;
    }
    if (descriptionElement) {
        descriptionElement.textContent = description;
    }
    if (progressElement) {
        progressElement.hidden = !showExtractionProgress;
    }

    overlay.hidden = false;
    document.body.classList.add('is-model-waiting');
}

function createExtractionProgressId() {
    if (window.crypto?.randomUUID) {
        return window.crypto.randomUUID();
    }
    return `extraction-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function startExtractionProgressStream(progressId) {
    return new Promise((resolve, reject) => {
        const eventSource = new EventSource(
            `/api/coffee-bean-card-extractions/${encodeURIComponent(progressId)}/events`
        );
        let connected = false;

        eventSource.addEventListener('connected', () => {
            connected = true;
            resolve(eventSource);
        }, {once: true});

        eventSource.addEventListener('progress', (event) => {
            try {
                const body = JSON.parse(event.data);
                updateExtractionProgress(body.phase);
                if (body.phase === 'COMPLETED' || body.phase === 'FAILED') {
                    eventSource.close();
                }
            } catch (error) {
                eventSource.close();
            }
        });

        eventSource.addEventListener('error', () => {
            if (!connected) {
                eventSource.close();
                reject(new Error('진행 상태 스트림 연결 실패'));
            }
        });
    });
}

async function submitExtractionForm(form, submitButton, progressId) {
    const resultUrl = window.location.href;
    let eventSource;

    try {
        eventSource = await startExtractionProgressStream(progressId);
        const response = await fetch(form.action, {
            method: 'POST',
            body: new FormData(form),
            headers: {'Accept': 'text/html'}
        });
        if (!response.ok) {
            throw new Error(`자동 입력 요청 실패: ${response.status}`);
        }

        const html = await response.text();
        eventSource?.close();
        document.open();
        document.write(html);
        document.close();
        window.history.replaceState({}, '', resultUrl);
    } catch (error) {
        eventSource?.close();
        form.removeAttribute('aria-busy');
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.textContent = submitButton.dataset.originalText || '사진으로 등록';
        }

        const titleElement = document.querySelector('[data-model-wait-title]');
        const descriptionElement = document.querySelector('[data-model-wait-description]');
        if (titleElement) {
            titleElement.textContent = '자동 입력을 완료하지 못했어요';
        }
        if (descriptionElement) {
            descriptionElement.textContent = '잠시 후 다시 시도해주세요.';
        }
    }
}

document.querySelectorAll('[data-card-extraction-form]').forEach((form) => {
    const submitButton = form.querySelector('button[type="submit"]');

    form.addEventListener('submit', (event) => {
        event.preventDefault();
        const progressId = createExtractionProgressId();
        let progressInput = form.querySelector('input[name="extractionProgressId"]');
        if (!progressInput) {
            progressInput = document.createElement('input');
            progressInput.type = 'hidden';
            progressInput.name = 'extractionProgressId';
            form.appendChild(progressInput);
        }
        progressInput.value = progressId;

        form.setAttribute('aria-busy', 'true');
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.dataset.originalText = submitButton.textContent || '';
            submitButton.textContent = '분석 중...';
        }
        showModelWaitOverlay('원두 카드를 분석하고 있어요', '완료될 때까지 잠시만 기다려주세요.', true);
        submitExtractionForm(form, submitButton, progressId);
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

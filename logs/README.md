# 백엔드 분석 로그

백엔드를 재시작하면 이 폴더에 로그가 생성됩니다. `sh/01_back_start.sh`는 실행 위치와 관계없이 이 폴더를 사용합니다. IDE 실행 시에는 작업 디렉터리를 `coffee`로 설정하거나 `BREWLOG_TRACE_DIRECTORY`를 이 폴더의 절대 경로로 지정하세요.

- `bean-extraction.jsonl`: 요청별 OCR·LLM·검증·최종 폼 값. 한 줄에 JSON 하나입니다.
- `backend.log`: 일반 서버 로그와 오류. `[extractionId=...]`로 요청을 연결합니다.
- `archive/`: 날짜·용량 기준 압축 보관. 최대 14일, 분석 로그 200MB / 서버 로그 100MB.

주요 단계: `llm.request.settings`, `request.start`, `ocr.start`, `ocr.result`, `llm.start`, `llm.prompt`, `llm.http.response`, `llm.json`, `llm.validated`, `llm.failure`, `llm.result`, `ocr.fallback.result`, `form.result`, `request.complete` / `request.failed`.

`requestId`가 같은 줄을 모으면 한 번의 분석을 확인할 수 있습니다. 단계별 경과 시간과 LLM 빈 응답 여부도 기록합니다. LLM 실패·빈 응답·출력 한도 초과 시 오류를 안내하며 OCR 추측값으로 폼을 채우지 않습니다. `llm.failure`와 `request.failed`를 확인하세요. OCR 보조 파서는 항상 실행되므로 `ocr.fallback.result` 자체가 LLM 실패를 뜻하지는 않습니다.

OCR 텍스트와 원두 정보가 포함되므로 생성 파일은 Git에서 제외됩니다. 인증 헤더·API 키·이미지 바이트는 분석 로그에 기록하지 않습니다. 운영 환경에서 비활성화하려면 `logging.level.coffee.extraction.trace=OFF`를 설정하세요.

OpenRouter 기본 모델은 `google/gemma-4-31b-it:free`이며 출력 한도는 2048토큰, 추론은 비활성화입니다. `BREWLOG_OPENROUTER_MODEL_ID`로 모델을, `BREWLOG_OPENROUTER_REASONING_ENABLED`로 추론 활성화 여부를 변경합니다. `BREWLOG_OPENROUTER_MAX_TOKENS`와 `BREWLOG_OPENROUTER_REASONING_EFFORT`로 조정할 수 있습니다. 실제 공급 모델에 따라 추론 설정 지원 여부가 다르므로 응답 잘림도 별도로 검사합니다.

로컬 LLM 공급자는 Groq로 전환했습니다. 키는 application-local.yml의 brewlog.llm.groq.api-key 또는 GROQ_API_KEY로 설정합니다. 기본 모델은 qwen/qwen3.8-27b이며 reasoning_effort=none, 최대 출력 768토큰으로 요청합니다. llm.request.settings에서 provider와 모델을 확인할 수 있습니다.

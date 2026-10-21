package com.hsg.coffee.domain.apiusage.service;

/** Kept separate from provider errors so fallbacks cannot swallow a quota denial. */
public class ApiLimitException extends RuntimeException {
    private final int status;
    private ApiLimitException(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
    public static ApiLimitException exhausted(String provider) {
        return new ApiLimitException(429, provider + " 서비스 전체 AI 사용 한도에 도달했습니다. 한도가 초기화된 후 다시 시도해주세요.");
    }
    public static ApiLimitException busy() {
        return new ApiLimitException(429, "현재 AI 요청을 처리 중입니다. 잠시 후 다시 시도해주세요.");
    }
    public static ApiLimitException unavailable() {
        return new ApiLimitException(503, "AI 사용량을 안전하게 확인할 수 없어 요청을 중지했습니다. 관리자 확인이 필요합니다.");
    }
}

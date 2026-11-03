package com.dotlo.line4thon_18team.preference;

import com.dotlo.line4thon_18team.global.llm.StructuredLlmExecutor;
import com.dotlo.line4thon_18team.global.llm.schema.SchemaLoader;
import com.dotlo.line4thon_18team.preference.ai.PreferenceInterpretationTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 취향 기능 빈 등록 (BE3).
 *
 * <p>저장소는 DB 구조가 정해지기 전까지 메모리 구현을 쓴다. JPA로 바꿀 때는 PreferenceRepository 빈만 교체하면 된다.
 */
@Configuration
public class PreferenceConfig {

    @Bean
    public PreferenceInterpretationTask preferenceInterpretationTask(SchemaLoader schemaLoader) {
        return new PreferenceInterpretationTask(schemaLoader);
    }

    @Bean
    public PreferenceRepository preferenceRepository() {
        return new InMemoryPreferenceRepository();
    }

    @Bean
    public PreferenceService preferenceService(PreferenceRepository preferenceRepository,
                                               StructuredLlmExecutor structuredLlmExecutor,
                                               PreferenceInterpretationTask preferenceInterpretationTask) {
        return new PreferenceService(preferenceRepository, structuredLlmExecutor, preferenceInterpretationTask,
                Clock.systemUTC());
    }
}

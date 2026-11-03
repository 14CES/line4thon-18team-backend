package com.dotlo.line4thon_18team.global.config;

import com.dotlo.line4thon_18team.global.llm.LlmClient;
import com.dotlo.line4thon_18team.global.llm.StructuredLlmExecutor;
import com.dotlo.line4thon_18team.global.llm.openai.OpenAiChatClient;
import com.dotlo.line4thon_18team.global.llm.schema.SchemaLoader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 공통 모듈 빈 등록 (BE3).
 *
 * <p>서버가 시작할 때는 OpenAI를 호출하지 않는다. API 키가 비어 있어도(CI 등) 서버는 정상으로 뜨고,
 * 실제 AI 기능을 호출하는 시점에만 키를 확인한다.
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    @Bean
    public LlmClient llmClient(LlmProperties properties) {
        return new OpenAiChatClient(properties.openai().toSettings());
    }

    @Bean
    public StructuredLlmExecutor structuredLlmExecutor(LlmClient llmClient, LlmProperties properties) {
        return new StructuredLlmExecutor(llmClient, properties.maxAttemptsOrDefault());
    }

    @Bean
    public SchemaLoader schemaLoader() {
        return new SchemaLoader();
    }
}

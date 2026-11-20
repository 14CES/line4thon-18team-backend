package com.dotlo.line4thon_18team.placeai;

import com.dotlo.line4thon_18team.global.llm.StructuredLlmExecutor;
import com.dotlo.line4thon_18team.global.llm.schema.SchemaLoader;
import com.dotlo.line4thon_18team.place.PlaceDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 장소 AI 답변 빈 등록 (BE3). */
@Configuration
public class PlaceAnswerConfig {

    @Bean
    public PlaceAnswerTask placeAnswerTask(SchemaLoader schemaLoader) {
        return new PlaceAnswerTask(schemaLoader);
    }

    @Bean
    public PlaceAnswerService placeAnswerService(PlaceDetails placeDetails, StructuredLlmExecutor llm,
                                                 PlaceAnswerTask task) {
        return new PlaceAnswerService(placeDetails, llm, task);
    }
}

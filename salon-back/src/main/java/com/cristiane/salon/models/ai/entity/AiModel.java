package com.cristiane.salon.models.ai.entity;

/** Modelos liberados pelo proxy LiteLLM da disciplina. */
public enum AiModel {
    GPT_4O_MINI("gpt-4o-mini"),
    GPT_4O("gpt-4o"),
    GPT_4_1_MINI("gpt-4.1-mini");

    private final String wireName;

    AiModel(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static AiModel fromWireName(String wireName) {
        for (AiModel model : values()) {
            if (model.wireName.equals(wireName)) {
                return model;
            }
        }
        throw new IllegalArgumentException("Modelo de IA não suportado: " + wireName);
    }
}

package com.team2.postservice.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team2.postservice.common.exception.AiException;
import com.team2.postservice.post.entity.PostCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;


public class AiDraftStatic {
    public static final Set<String> SERVER_FIELDS = Set.of("totalAmount", "startDate", "endDate");

    public static final Map<String, Integer> TERMS = Map.ofEntries(
            Map.entry("title",120), Map.entry("scope",4000), Map.entry("exclusions",2000), Map.entry("materials",2000),
            Map.entry("totalAmount",32), Map.entry("paymentTerms",2000), Map.entry("startDate",10), Map.entry("endDate",10),
            Map.entry("workLocation",1000), Map.entry("acceptanceCriteria",2000), Map.entry("warrantyTerms",2000),
            Map.entry("cancellationTerms",2000), Map.entry("additionalCostTerms",2000));

    public static void fillServerFields(JsonNode result, Map<String, String> sources, Map<String, String> current) {
        if (!result.path("suggestedTerms").isObject() || !result.path("fieldSources").isObject()) {
            throw AiException.output();
        }

        ObjectNode terms = (com.fasterxml.jackson.databind.node.ObjectNode) result.path("suggestedTerms");
        ObjectNode evidence = (com.fasterxml.jackson.databind.node.ObjectNode) result.path("fieldSources");

        for (String field : SERVER_FIELDS) {
            String value = field.equals("totalAmount") ? sources.get("PROPOSAL_AMOUNT") : current.get(field);
            if (value != null && value.isBlank()) {
                value = null;
            }
            terms.put(field, value);
            evidence.putObject(field).put(
                            "sourceId", value == null ? "SUGGESTED_CLAUSE" : field.equals("totalAmount") ? "PROPOSAL_AMOUNT" : "USER_" + field)
                    .put("quote", value == null ? "" : value);
        }
    }

    public static void inputText(String value, int max) {
        if (value == null || value.length() > max) {
            throw AiException.input("입력 길이를 확인해주세요 (최대 " + max + "자).");
        }
    }

    public static void validateTerms(Map<String, String> terms, boolean output) {
        try {
            if (terms == null || !TERMS.keySet().containsAll(terms.keySet())) throw new IllegalArgumentException();
            for (Map.Entry<String, String> entry : terms.entrySet()) {
                String value = entry.getValue();

                if (value == null || value.isBlank()) {
                    continue;
                }

                if (value.length() > TERMS.get(entry.getKey())) {
                    throw new IllegalArgumentException();
                }

                if (entry.getKey().equals("totalAmount")) {
                    if (!value.matches("[0-9]{1,10}(\\.[0-9]{1,2})?") || new BigDecimal(value).signum() <= 0) {
                        throw new IllegalArgumentException();
                    }
                }
                if (entry.getKey().equals("startDate") || entry.getKey().equals("endDate")) {
                    if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        throw new IllegalArgumentException();
                    }
                    LocalDate.parse(value);
                }
            }
            if (terms.get("startDate") != null && !terms.get("startDate").isBlank() && terms.get("endDate") != null && !terms.get("endDate").isBlank()
                    && LocalDate.parse(terms.get("endDate")).isBefore(LocalDate.parse(terms.get("startDate")))) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException e) {
            if (output) {
                throw AiException.output();
            } throw AiException.input("계약 항목의 길이, 금액 또는 날짜를 확인해주세요."); }
    }

    static Map<String, Object> textSchema(int max) { return Map.of("type", "string", "maxLength", max); }
    static Map<String, Object> nullableText(int max) { return Map.of("type", List.of("string", "null"), "maxLength", max); }
    static Map<String, Object> arraySchema() { return Map.of("type", "array", "maxItems", 15, "items", textSchema(500)); }
    static Map<String, Object> objectSchema(Map<String, Object> properties) {
        return Map.of("type", "object", "properties", properties, "required", new ArrayList<>(properties.keySet()), "additionalProperties", false);
    }


    public static Map<String, Object> postSchema() {
        return objectSchema(
                Map.of(
                        "suggestion", objectSchema(
                                Map.of(
                                        "title", textSchema(100),
                                        "content", textSchema(800),
                                        "category",
                                        Map.of(
                                                "type", "string",
                                                "enum", Arrays.stream(PostCategory.values()
                                                ).map(Enum::name).toList()
                                        )
                                )
                        )
                )
        );
    }

    public static Map<String, Object> postContentRevisionSchema() {
        return objectSchema(Map.of("replacement", textSchema(800)));
    }

    public static String validatePostContentRevision(JsonNode result) {
        keys(result, Set.of("replacement"));
        String replacement = text(result.path("replacement"), 800, false);
        if (replacement == null || replacement.isBlank()) throw AiException.output();
        return replacement;
    }

    public static Map<String, Object> contractSchema(Set<String> sourceIds) {
        Map<String, Object> terms = new LinkedHashMap<>(), sources = new LinkedHashMap<>();

        ArrayList<String> allowed = new ArrayList<>(sourceIds); allowed.add("SUGGESTED_CLAUSE");

        TERMS.forEach((field, max) -> {
            if (SERVER_FIELDS.contains(field)) return;
            terms.put(field, nullableText(Math.min(max, 500)));
            sources.put(
                    field,
                    objectSchema(
                            Map.of("sourceId", Map.of("type", "string", "enum", allowed), "quote", textSchema(2000))
                    )
            );
        });

        return objectSchema(Map.of("suggestedTerms", objectSchema(terms), "fieldSources", objectSchema(sources), "conflicts", arraySchema(), "warnings", arraySchema()));
    }

    public static void keys(JsonNode node, Set<String> expected) {
        if (!node.isObject()) {
            throw AiException.output();
        }

        Set<String> actual = new HashSet<>(); node.fieldNames().forEachRemaining(actual::add);

        if (!actual.equals(expected)) {
            throw AiException.output();
        }
    }

    public static String text(JsonNode node, int max, boolean nullable) {
        if (nullable && node.isNull()) {
            return null;
        }
        if (!node.isTextual() || node.textValue().length() > max) {
            throw AiException.output();
        }
        return node.textValue();
    }

    public static void strings(JsonNode node) {
        if (!node.isArray() || node.size() > 15) {
            throw AiException.output();
        }
        node.forEach(value -> text(value, 500, false));
    }

    public static void validateContract(JsonNode result, Map<String, String> sources, Map<String, String> current) {
        keys(result, Set.of("suggestedTerms", "fieldSources", "conflicts", "warnings"));

        JsonNode termsNode = result.path("suggestedTerms"); keys(termsNode, TERMS.keySet()); keys(result.path("fieldSources"), TERMS.keySet());

        Map<String, String> terms = new HashMap<>(); List<String> missing = new ArrayList<>();

        TERMS.forEach((field, max) -> {
            String value = text(termsNode.path(field), SERVER_FIELDS.contains(field) ? max : Math.min(max, 500), true);
            terms.put(field, value);

            JsonNode evidence = result.path("fieldSources").path(field); keys(evidence, Set.of("sourceId", "quote"));

            String source = text(evidence.path("sourceId"),100,false), quote = text(evidence.path("quote"),2000,false);

            if (!Objects.requireNonNull(source).equals("SUGGESTED_CLAUSE") && !sources.containsKey(source)) {
                throw AiException.output();
            }

            if (value == null || value.isBlank()) {
                missing.add(field); return;
            }

            if (source.equals("SUGGESTED_CLAUSE")) {
                if (!Objects.requireNonNull(quote).isEmpty()) {
                    throw AiException.output();
                }
            }
            else if (Objects.requireNonNull(quote).isBlank() || !sources.get(source).contains(quote)) {
                throw AiException.output();
            }

            if (field.equals("totalAmount") &&
                    (!value.equals(sources.get("PROPOSAL_AMOUNT")) ||
                    !source.equals("PROPOSAL_AMOUNT"))) {
                throw AiException.output();
            }

            if (Set.of("startDate","endDate").contains(field) &&
                    (!value.equals(current.get(field)) || !source.equals("USER_" + field))) {
                throw AiException.output();
            }
        });
        validateTerms(terms, true);
        strings(result.path("conflicts"));
        strings(result.path("warnings"));
        ArrayNode missingArray = ((ObjectNode) result).putArray("missingFields");
        missing.forEach(missingArray::add);
    }

    public static void validatePost(JsonNode result) {
        keys(result, Set.of("suggestion"));
        JsonNode suggestion = result.path("suggestion"); keys(suggestion, Set.of("title", "content", "category"));

        if (Objects.requireNonNull(text(suggestion.path("title"), 100, false)).isBlank() ||
                Objects.requireNonNull(text(suggestion.path("content"), 800, false)).isBlank()) {
            throw AiException.output();
        }

        try {
            PostCategory.valueOf(text(suggestion.path("category"),50,false));
        } catch (IllegalArgumentException e) {
            throw AiException.output();
        }
    }


}

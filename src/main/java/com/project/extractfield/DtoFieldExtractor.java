package com.project.extractfield;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;

public class DtoFieldExtractor implements ActionListener {

	private final JBTextField inputSelectField;
	private final JBTextField inputDtoField;
	private final JBTextArea resultArea;

	public DtoFieldExtractor(JBTextField inputSelectField, JBTextField inputDtoField, JBTextArea resultArea) {
		this.inputSelectField = inputSelectField;
		this.inputDtoField = inputDtoField;
		this.resultArea = resultArea;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String selectQuery = inputSelectField.getText();
		String dtoText = inputDtoField.getText();

		// 실제 로직을 여기에 구현
		String result = "입력된 SELECT: " + selectQuery + "\n입력된 DTO: " + dtoText;
		resultArea.setText(result);

		System.out.println("logic 버튼 실행완료");
	}

	// 태그 및 주석 제거
	private static String removeTagsComments(String str) {
		str = str.replaceAll("<!\\[CDATA\\[", "")
			.replaceAll("]]>", "")
			.replaceAll("<[^>]*>", "")
			.replaceAll("--.*", "")
			.replaceAll("/\\*.*?\\*/", "")
			.replaceAll("\\s+", " ")
			.replaceAll("SELECT .*?\\*/", " ");
		return str.trim();
	}

	// 필드 추출
	private static List<String> extractFieldNames(String selectStr) {
		String cleaned = removeTagsComments(selectStr);

		List<String> chunks = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		int depth = 0;

		for (char c : cleaned.toCharArray()) {
			if (c == ',' && depth == 0) {
				chunks.add(current.toString().trim());
				current.setLength(0);
			} else {
				current.append(c);
				if (c == '(')
					depth++;
				else if (c == ')')
					depth--;
			}
		}
		if (!current.toString().isBlank())
			chunks.add(current.toString().trim());

		return chunks.stream()
			.map(DtoFieldExtractor::extractField)
			.map(DtoFieldExtractor::snakeToCamel)
			.collect(Collectors.toList());
	}

	private static String extractField(String str) {
		String upper = str.toUpperCase();

		if (upper.contains(" AS ")) {
			return upper.split(" AS ")[1].trim();
		} else {
			String lastToken = str.trim().split("\\s+")[str.trim().split("\\s+").length - 1];
			if (lastToken.contains(".")) {
				return lastToken.substring(lastToken.indexOf('.') + 1).trim();
			} else {
				return lastToken.trim();
			}
		}
	}

	private static String snakeToCamel(String str) {
		String[] parts = str.toLowerCase().split("_");
		StringBuilder result = new StringBuilder(parts[0]);
		for (int i = 1; i < parts.length; i++) {
			result.append(Character.toUpperCase(parts[i].charAt(0)))
				.append(parts[i].substring(1));
		}
		return result.toString();
	}

	private static List<String> retrieveFieldDeclarations(List<String> fieldNames, String dtoPath) throws IOException {
		List<String> result = new ArrayList<>();
		List<String> lines = Files.readAllLines(Paths.get(dtoPath));
		String lastComment = "";

		for (String line : lines) {
			String cleaned = line.strip();

			if (cleaned.startsWith("/**") || cleaned.startsWith("/*") || cleaned.startsWith("*") || cleaned.startsWith(
				"//")) {
				lastComment = cleaned.replaceAll("^\\s*(/\\*+|\\*+|//)", "").replaceAll("\\*/$", "").strip();
			}

			if (cleaned.startsWith("private")) {
				for (String field : fieldNames) {
					if (cleaned.toLowerCase().contains(field.toLowerCase())) {
						result.add(cleaned + (lastComment.isEmpty() ? "" : " // " + lastComment));
						lastComment = "";
						break;
					}
				}
			}
		}

		// Oracle Audit 필드 추가
		List<String> auditFields = List.of(
			"private String frstEntrEmpno; // 최초입력자사번",
			"private LocalDateTime frstEntrDtm; // 최초입력일시",
			"private String finlUpidEmpno; // 최종수정자사번",
			"private LocalDateTime finlUpdtDtm; // 최종수정일시"
		);
		for (String audit : auditFields) {
			String name = audit.split(" ")[2].replace(";", "");
			if (fieldNames.contains(name)) {
				result.add(audit);
			}
		}

		return result;
	}
}

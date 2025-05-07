package com.project.extractfield;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.*;


public class DtoFieldExtractor implements ActionListener {

	private final JCheckBox auditChkBox;
	private final JTextArea inputSelectField;
	private final JTextArea inputDtoField;
	private final JTextArea resultArea;

	public DtoFieldExtractor(JCheckBox auditChkBox, JTextArea inputSelectField, JTextArea inputDtoField, JTextArea resultArea) {
		this.auditChkBox = auditChkBox;
		this.inputSelectField = inputSelectField;
		this.inputDtoField = inputDtoField;
		this.resultArea = resultArea;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String selectStr = inputSelectField.getText();
		String dtoStr = inputDtoField.getText();

		// 실제 로직을 여기에 구현
		System.out.println("actionPerformed 시작");
		List<String> fieldNames = extractFieldNames(selectStr);
		List<String> declarations = retrieveFieldDeclarations(auditChkBox.isSelected(), fieldNames, dtoStr);
		String resuultTemp = declarations.stream().collect(Collectors.joining("\n"));
		resultArea.setText(resuultTemp);
		resultArea.setVisible(true);
		System.out.println("actionPerformed 종료. resuultTemp : " + resuultTemp);
	}

	// 필드 추출
	private static List<String> extractFieldNames(String selectStr) {
		String cleanSelectStr = removeTagsComments(selectStr);

		List<String> chunks = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		int depth = 0;

		for (char c : cleanSelectStr.toCharArray()) {
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

	// 태그 및 주석 제거
	private static String removeTagsComments(String selectStr) {
		return selectStr
			.replaceAll("<!\\[CDATA\\[", "")
			.replaceAll("]]>", "")
			.replaceAll("<[^>]*>", "")
			.replaceAll("--.*", "")
			.replaceAll("/\\*.*?\\*/", "")
			.replaceAll("\\s+", " ")
			.replaceAll("SELECT .*?\\*/", " ")
			.trim();
	}

	private static String extractField(String str) {
		String upper = str.toUpperCase();

		if (upper.contains(" AS ")) {
			String[] asSplit = upper.split(" AS ");
			String finalChunk = asSplit[asSplit.length - 1].trim();
			return finalChunk;
			// // 서브쿼리에 AS 가 있으나 해당 서브쿼리에 대한 AS 키워드는 생략된 경우
			// if(finalChunk.contains(" ")) {
			// 	String[] blankSplit = finalChunk.split(" ");
			// 	return blankSplit[blankSplit.length -1].trim();
			// } else {
			// 	return finalChunk;
			// }
		} else {
			String[] blankSplit = str.split("\\s+");
			String lastWord = blankSplit[blankSplit.length - 1];
			if (lastWord.contains(".")) {
				return lastWord.substring(lastWord.indexOf('.') + 1);
			} else {
				return lastWord;
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

	private static List<String> retrieveFieldDeclarations(boolean needAudit, List<String> fieldNames, String dtoStr) {
		List<String> result = new ArrayList<>();
		List<String> lines = Arrays.asList(dtoStr.split("\\R")); // 줄 단위로 분리
		String lastComment = "";

		for (String line : lines) {
			String cleaned = line.strip();

			if (cleaned.startsWith("/**") || cleaned.startsWith("/*") || cleaned.startsWith("*")
				|| cleaned.startsWith("//") && !cleaned.startsWith("*/")) {
				lastComment = cleaned.replaceAll("^\\s*(/\\*+|\\*+|//)", "").replaceAll("\\*/$", "").strip();
			}

			if (cleaned.startsWith("private")) {
				for (String field : fieldNames) {
					String regex = "\\s+" + Pattern.quote(field.toLowerCase()) + "(\\s+|;)";
					if (cleaned.toLowerCase().matches(".*" + regex + ".*")) {
						if("".equals(lastComment)) {
							result.add(cleaned);
						} else {
							result.add(cleaned + (lastComment.isEmpty() ? "" : " // " + lastComment));
							lastComment = "";
						}
						break;
					}
				}
			}
		}

		if(needAudit) {
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
		}

		return result;
	}
}

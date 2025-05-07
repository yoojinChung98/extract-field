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

	public DtoFieldExtractor(JCheckBox auditChkBox, JTextArea inputSelectField, JTextArea inputDtoField,
		JTextArea resultArea) {
		this.auditChkBox = auditChkBox;
		this.inputSelectField = inputSelectField;
		this.inputDtoField = inputDtoField;
		this.resultArea = resultArea;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String selectStr = inputSelectField.getText();
		String dtoStr = inputDtoField.getText();

		List<String> fieldNames = extractFieldNames(selectStr);
		List<String> declarations = retrieveFieldDeclarations(auditChkBox.isSelected(), fieldNames, dtoStr);
		String resuultTemp = declarations.stream().collect(Collectors.joining("\n"));
		resultArea.setText(resuultTemp);
		resultArea.setVisible(true);
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

		// AS 키워드가 있는 경우 : AS 로 split 후 가장 마지막 문자열 사용
		if (upper.contains(" AS ")) {
			String[] asSplit = upper.split(" AS ");
			String lastChunk = asSplit[asSplit.length - 1].trim();
			if (lastChunk.contains(" ")) {
				// 서브쿼리에 AS 가 있으나 해당 서브쿼리에 대한 AS 키워드는 생략된 경우
				String[] blankSplit = lastChunk.split(" ");
				return blankSplit[blankSplit.length - 1].trim();
			} else {
				return lastChunk;
			}
			// AS 키워드가 없는 경우 : 공백으로 split 후 가장 마지막 문자열 사용
		} else {
			String[] blankSplit = str.split("\\s+");
			String lastChunk = blankSplit[blankSplit.length - 1];
			if (lastChunk.contains(".")) {
				return lastChunk.substring(lastChunk.indexOf('.') + 1);
			} else {
				return lastChunk;
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
			String cleanedLine = line.strip();

			lastComment = takeComment(cleanedLine, lastComment);

			if (cleanedLine.startsWith("private")) {
				for (String field : fieldNames) {
					if (lineContainField(cleanedLine, field)) {
						result.add(cleanedLine + (lastComment.isEmpty() ? "" : " // " + lastComment));
						lastComment = "";
						break;
					}
				}
			}
		}

		// ORACLE Auidt 컬럼 관련 필드 추가
		if (needAudit) {
			addAuditField(result, fieldNames);
		}

		return result;
	}

	private static String takeComment(String sourceStr, String lastComment) {

		if (isComment(sourceStr)) {
			String comment = sourceStr.replaceAll("^\\s*(/\\*+|\\*+|//)", "").replaceAll("\\*/$", "").strip();
			return lastComment + comment;
		}
		return lastComment;
	}

	private static boolean isComment(String targetStr) {
		if(targetStr.startsWith("*/")) {
			return false;
		} else if (targetStr.startsWith("/**")
			|| targetStr.startsWith("/*")
			|| targetStr.startsWith("*")
			|| targetStr.startsWith("//")) {
			return true;
		}
		return false;
	}

	private static boolean lineContainField(String cleanLine, String field) {
		// 정규식 : '하나 이상의 공백 + 필드명(lowercase) + 하나 이상의 공백 + ;'
		String matchStr = "\\s+" + Pattern.quote(field.toLowerCase()) + "(\\s+|;)";
		String lowerLine = cleanLine.toLowerCase();

		// matchStr 이 lowerLine 어디에든 존재한다면 true
		return lowerLine.matches(".*" + matchStr + ".*");
	}

	private static void addAuditField(List<String> targetList, List<String> fieldNames) {
		List<String> auditFields = List.of(
			"private String frstEntrEmpno; // 최초입력자사번",
			"private LocalDateTime frstEntrDtm; // 최초입력일시",
			"private String finlUpidEmpno; // 최종수정자사번",
			"private LocalDateTime finlUpdtDtm; // 최종수정일시"
		);

		for (String audit : auditFields) {
			String name = audit.split(" ")[2].replace(";", "");
			for (String field : fieldNames) {
				if (name.equalsIgnoreCase(field)) {
					targetList.add(audit);
				}
			}
		}
	}
}

package com.project.extractfield;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

public class ExtractFieldToolWindowFactory implements ToolWindowFactory, DumbAware {

	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {



		// 입력 필드
		JTextArea inputSelectField = new JTextArea();
		JTextArea inputDtoField = new JTextArea();
		changeDefaultFont(inputSelectField);
		changeDefaultFont(inputDtoField);
		JScrollPane selectScroll = elaborateScrollPanel(inputSelectField, "SELECT 구문만 입력");
		JScrollPane dtoScroll = elaborateScrollPanel(inputDtoField, "Result DTO 전체 입력");
		JCheckBox auditChkBox = new JCheckBox("Audit 필드 출력");
		auditChkBox.setSelected(false);

		// 결과 출력 영역
		JTextArea resultArea = new JTextArea();
		changeDefaultFont(resultArea);
		resultArea.setEditable(false); // 읽기 전용으로 설정
		resultArea.setLineWrap(true);  // 줄바꿈
		resultArea.setVisible(false);
		JScrollPane resultScroll = elaborateScrollPanel(resultArea, "결과");
		resultScroll.setPreferredSize(new Dimension(0, 60)); // 높이 50px (폭은 아래 레이아웃에서 조절됨)


		// 버튼 생성
		JButton runLogicBtn = new JButton("필드 추출");
		JButton copyResultBtn = new JButton("결과 복사");
		// 버튼 영역 패널 (수평 정렬)
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(runLogicBtn);
		buttonPanel.add(copyResultBtn);

		// 패널 생성
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		addLabelAndContentIn(panel, selectScroll);
		addLabelAndContentIn(panel, dtoScroll);
		addLabelAndContentIn(panel, auditChkBox);
		addLabelAndContentIn(panel, buttonPanel);
		addLabelAndContentIn(panel, resultScroll);

		// 버튼 이벤트 핸들링
		runLogicBtn.addActionListener(new DtoFieldExtractor(auditChkBox, inputSelectField, inputDtoField, resultArea));

		copyResultBtn.addActionListener(e -> {
			String resultText = resultArea.getText();
			if (!resultText.isEmpty()) {
				Toolkit.getDefaultToolkit()
					.getSystemClipboard()
					.setContents(new StringSelection(resultText), null);
			}
			System.out.println("copy 종료");
		});

		// ToolWindow에 등록
		Content content = ContentFactory.getInstance().createContent(panel, "", false);
		toolWindow.getContentManager().addContent(content);

		System.out.println("Current file.encoding: " + System.getProperty("file.encoding"));

	}

	private JScrollPane elaborateScrollPanel(JTextArea textArea, String label) {
		textArea.setLineWrap(true);
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setPreferredSize(new Dimension(0, 30));
		scrollPane.setBorder(BorderFactory.createTitledBorder(label));
		return scrollPane;
	}

	private void addLabelAndContentIn(JPanel targetPanel, JPanel content) {
		targetPanel.add(content);
		targetPanel.add(Box.createVerticalStrut(10));
	}

	private void addLabelAndContentIn(JPanel targetPanel, JCheckBox content) {
		targetPanel.add(content);
		targetPanel.add(Box.createVerticalStrut(10));
	}

	private void addLabelAndContentIn(JPanel targetPanel, JScrollPane content) {
		targetPanel.add(content);
		targetPanel.add(Box.createVerticalStrut(10));
	}

	private void changeDefaultFont(JTextArea textArea) {
		String os = System.getProperty("os.name").toLowerCase();
		Font font;
		if (os.contains("win")) {
			font = new Font("Malgun Gothic", Font.PLAIN, 13);
		} else if (os.contains("mac")) {
			font = new Font("Apple SD Gothic Neo", Font.PLAIN, 13);
		} else {
			font = new Font("NanumGothic", Font.PLAIN, 13); // 또는 UnDotum
		}
		textArea.setFont(font);
	}
}

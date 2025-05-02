package com.project.extractfield;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

import javax.swing.*;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ExtractFieldToolWindowFactory implements ToolWindowFactory, DumbAware {

	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

		// 입력 필드
		JBTextField inputSelectField = new JBTextField();
		JBTextField inputDtoField = new JBTextField();

		// 결과 출력 영역
		JBTextArea resultArea = new JBTextArea();
		resultArea.setEditable(false); // 읽기 전용으로 설정
		resultArea.setLineWrap(true);  // 줄바꿈
		resultArea.setBorder(BorderFactory.createLineBorder(Color.GRAY)); // 테두리 추가

		// 버튼 생성
		JButton runLogicBtn = new JButton("로직 실행");
		JButton copyResultBtn = new JButton("결과 복사");
		// 버튼 영역 패널 (수평 정렬)
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(runLogicBtn);
		buttonPanel.add(Box.createHorizontalStrut(10));
		buttonPanel.add(copyResultBtn);

		// 패널 생성
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		addLabelAndContentIn(panel, "SELECT 구문만 입력", inputSelectField);
		addLabelAndContentIn(panel, "Result DTO 입력", inputDtoField);
		addLabelAndContentIn(panel, null, buttonPanel);
		addLabelAndContentIn(panel, "결과", resultArea);

		// 버튼 이벤트 핸들링
		runLogicBtn.addActionListener(e -> {
			String selectQuery = inputSelectField.getText();
			String dtoText = inputDtoField.getText();

			// 예시: 결과 생성 로직 (간단하게 출력만)
			String result = "입력된 SELECT: " + selectQuery + "\n입력된 DTO: " + dtoText;
			resultArea.setText(result);
			System.out.println("logic 버튼 실행완료");
		});
		// runLogicBtn.addActionListener(new ExtractFieldLogic(inputSelectField, inputDtoField, resultArea));

		copyResultBtn.addActionListener(e -> {
			String resultText = resultArea.getText();
			if (!resultText.isEmpty()) {
				Toolkit.getDefaultToolkit()
					.getSystemClipboard()
					.setContents(new StringSelection(resultText), null);
			}
			System.out.println("copy 결과");
			System.out.println(resultText);
		});

		// ToolWindow에 등록
		Content content = ContentFactory.getInstance().createContent(panel, "", false);
		toolWindow.getContentManager().addContent(content);
	}




	private void addLabelAndContentIn(JPanel targetPanel, String label, JBTextField content) {
		if(label != null) {
			targetPanel.add(new JLabel(label));
		}
		targetPanel.add(content);
		targetPanel.add(Box.createVerticalStrut(10));
	}

	private void addLabelAndContentIn(JPanel targetPanel, String label, JPanel content) {
		if(label != null) {
			targetPanel.add(new JLabel(label));
		}
		targetPanel.add(content);
		targetPanel.add(Box.createVerticalStrut(10));
	}

	private void addLabelAndContentIn(JPanel targetPanel, String label, JBTextArea content) {
		if(label != null) {
			targetPanel.add(new JLabel(label));
		}
		targetPanel.add(content);
		targetPanel.add(Box.createVerticalStrut(10));
	}
}

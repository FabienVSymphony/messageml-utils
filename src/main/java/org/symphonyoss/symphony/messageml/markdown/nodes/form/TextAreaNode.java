package org.symphonyoss.symphony.messageml.markdown.nodes.form;

/**
 * Class that Represents a Markdown Node for the "TextArea" form element.
 *
 * @author Lucas Macedo
 * @since 10/21/2019
 */
public class TextAreaNode extends FormElementNode implements PlaceholderLabelTooltipNode {
  private final static String MARKDOWN = "Text Area";

  private String placeholder;
  private String initialValue;
  private String label;
  private String tooltip;

  public TextAreaNode(String placeholder, String initialValue, String label, String tooltip) {
    super(MARKDOWN);
    this.placeholder = placeholder;
    this.initialValue = initialValue;
    this.label = label;
    this.tooltip = tooltip;
  }

  TextAreaNode(String markdown, String placeholder, String initialValue, String label, String tooltip) {
    super(markdown);
    this.placeholder = placeholder;
    this.initialValue = initialValue;
    this.label = label;
    this.tooltip = tooltip;
  }
  
  @Override
  public String getText() {
    StringBuilder markdownRepresentation = new StringBuilder();
    
    markdownRepresentation.append(generateMarkdownPlaceholderLabelAndTooltip(placeholder, label, tooltip));
    
    if(initialValue != null) {
      markdownRepresentation.append(markdownRepresentation.length() == 0 ? ":" : ""); 
      markdownRepresentation.append(initialValue);
    }

    return markdownRepresentation.toString();
  }
}

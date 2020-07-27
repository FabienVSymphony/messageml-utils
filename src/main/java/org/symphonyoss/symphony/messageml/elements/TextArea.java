package org.symphonyoss.symphony.messageml.elements;

import org.commonmark.node.Node;
import org.symphonyoss.symphony.messageml.MessageMLParser;
import org.symphonyoss.symphony.messageml.exceptions.InvalidInputException;
import org.symphonyoss.symphony.messageml.markdown.nodes.form.TextAreaNode;

import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Class representing a Text Area inside a Form.
 * @author Sandro Ribeiro
 * @since 06/12/2019
 */
public class TextArea extends FormElement implements RegexElement, LabelableElement, TooltipableElement {

  public static final String MESSAGEML_TAG = "textarea";

  private static final String PLACEHOLDER_ATTR = "placeholder";
  private static final String REQUIRED_ATTR = "required";
  private static final List<String> VALID_VALUES_FOR_REQUIRED_ATTR = Arrays.asList("true", "false");

  private static final String MARKDOWN = "Text Area";

  public TextArea(Element parent, FormatEnum format) {
    super(parent, MESSAGEML_TAG, format);
  }

  @Override
  public void validate() throws InvalidInputException {
    super.validate();

    if (getAttribute(NAME_ATTR) == null) {
      throw new InvalidInputException("The attribute \"name\" is required");
    }

    if (getAttribute(REQUIRED_ATTR) != null) {
      assertAttributeValue(REQUIRED_ATTR, VALID_VALUES_FOR_REQUIRED_ATTR);
    }

    assertAttributeNotBlank(NAME_ATTR);
    assertContentModel(Collections.singleton(TextNode.class));
  }

  @Override
  protected void buildAttribute(MessageMLParser parser,
      org.w3c.dom.Node item) throws InvalidInputException {
    switch (item.getNodeName()) {
      case NAME_ATTR:
      case REQUIRED_ATTR:
      case PLACEHOLDER_ATTR:
      case PATTERN_ATTR:
      case LABEL:
      case TITLE:
        setAttribute(item.getNodeName(), getStringAttribute(item));
        break;
      case PATTERN_ERROR_MESSAGE_ATTR:
        if(this.format != FormatEnum.MESSAGEML){
          throwInvalidInputException(item);
        }
        setAttribute(PATTERN_ERROR_MESSAGE_ATTR, getStringAttribute(item));
        break;
      case PRESENTATIONML_PATTERN_ERROR_MESSAGE_ATTR:
        if(this.format != FormatEnum.PRESENTATIONML){
          throwInvalidInputException(item);
        }
        setAttribute(PRESENTATIONML_PATTERN_ERROR_MESSAGE_ATTR, getStringAttribute(item));
        break;
      case ID_ATTR:
        if(this.format != FormatEnum.PRESENTATIONML){
          throwInvalidInputException(item);
        }
        fillAttributes(parser, item);
        break;
      default:
        throwInvalidInputException(item);
    }
  }

  @Override
  public Node asMarkdown() {
      return new TextAreaNode(getAttribute(PLACEHOLDER_ATTR), hasExactNumberOfChildren(1) ? getChild(0).asText() : null,
          getAttribute(LABEL), getAttribute(TITLE));
  }

  @Override
  public String getElementId(){
    return MESSAGEML_TAG;
  }

}

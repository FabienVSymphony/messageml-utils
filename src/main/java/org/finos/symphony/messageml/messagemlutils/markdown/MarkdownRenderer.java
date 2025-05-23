/*
 * Copyright 2016-2017 MessageML - Symphony LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.finos.symphony.messageml.messagemlutils.markdown;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.commonmark.node.*;
import org.commonmark.renderer.text.TextContentWriter;
import org.finos.symphony.messageml.messagemlutils.elements.MessageML;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.DateTimeNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.EmojiNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.KeywordNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.MentionNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.PreformattedNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.TableCellNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.TableNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.TableRowNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.TagNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.form.ButtonNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.form.FormElementNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.form.FormNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.form.OptionNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.form.PersonSelectorNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.form.SelectNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.form.TextAreaNode;
import org.finos.symphony.messageml.messagemlutils.markdown.nodes.form.TextFieldNode;
import org.finos.symphony.messageml.messagemlutils.util.XmlPrintStream;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used for converting MessageMLV2 to legacy formats. Renders Commonmark {@link Node}s generated by
 * {@link MessageML}.asMarkdown() as their text (Markdown) representation
 * and associated entity data in JSON format.
 * @author lukasz
 * @since 3/30/17
 */
public class MarkdownRenderer extends AbstractVisitor {

  private static final String TEXT = "text";
  private static final String EXPANDED_URL = "expandedUrl";
  private static final String ID = "id";
  private static final String INDEX_START = "indexStart";
  private static final String INDEX_END = "indexEnd";
  private static final String TYPE = "type";
  private static final String DATA = "data";
  private static final String VALUE = "value";
  private static final String FORMAT = "format";
  private static final String SCREEN_NAME = "screenName";
  private static final String PRETTY_NAME = "prettyName";
  private static final String USER_TYPE = "userType";
  private static final String USER_MENTIONS = "userMentions";
  private static final String HASHTAGS = "hashtags";
  private static final String URLS = "urls";
  private static final String DATETIMES = "datetimes";
  private static final String INDENT = "  ";

  private static final Pattern NOESCAPE_PATTERN = Pattern.compile("^\\s*([_*\\-+`])\\1*\\s*$");

  private final TrackingWriter writer = new TrackingWriter(new StringBuilder());
  private final ObjectNode json = new ObjectNode(JsonNodeFactory.instance);

  private boolean removeNewlines = true;
  private Character bulletListMarker;
  private int bulletListLevel = 0;
  private Integer orderedListCounter;
  private Character orderedListDelimiter;
  private int orderedListLevel = 0;

  /**
   * Process the document tree and generate its text representation.
   * @param document the input document tree.
   */
  public MarkdownRenderer(Document document) {
    visit(document);
  }

  @Override
  public void visit(Document document) {
    visitChildren(document);
  }

  @Override
  public void visit(Text text) {
    String content = text.getLiteral();
    if (removeNewlines) {
      content = XmlPrintStream.removeNewLines(content);
    }
    writer.write(addEscapeCharacter(content));
  }


  @Override
  public void visit(HardLineBreak hardLineBreak) {
    writer.line();
  }

  @Override
  public void visit(Paragraph paragraph) {
    writer.doubleLine();
    if (paragraph.getFirstChild() != null) {
      visitChildren(paragraph);
      writer.doubleLine();
    }
  }

  @Override
  public void visit(Emphasis em) {
    visitDelimited(em);
  }

  @Override
  public void visit(StrongEmphasis b) {
    visitDelimited(b);
  }

  @Override
  public void visit(Link a) {
    String href = a.getDestination();
    String title = StringUtils.defaultIfBlank(a.getTitle(), a.getDestination());
    String markdown = MessageFormat.format("[ {0} ]({1})", title, href);

    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    node.put(ID, href);
    node.put(TYPE, "URL");
    node.put(INDEX_END, writer.length() + markdown.length());
    node.put(INDEX_START, writer.length());
    node.put(TEXT, title);
    node.put(EXPANDED_URL, href);
    putJsonObject(URLS, node);

    writer.write(markdown);
  }

  @Override
  public void visit(BulletList ul) {
    writer.line();

    Character previousMarker = bulletListMarker;
    int previousLevel = bulletListLevel;

    bulletListLevel += (ul.getParent() instanceof ListItem) ? 1 : 0;

    bulletListMarker = ul.getBulletMarker();
    visitChildren(ul);
    writer.line();

    bulletListMarker = previousMarker;
    bulletListLevel = previousLevel;

    writer.line();
  }

  @Override
  public void visit(org.commonmark.node.OrderedList ol) {
    writer.line();

    Integer previousCounter = orderedListCounter;
    Character previousDelimiter = orderedListDelimiter;
    int previousLevel = orderedListLevel;

    orderedListLevel += (ol.getParent() instanceof ListItem) ? 1 : 0;

    orderedListCounter = ol.getStartNumber();
    orderedListDelimiter = ol.getDelimiter();
    visitChildren(ol);
    writer.line();

    orderedListCounter = previousCounter;
    orderedListDelimiter = previousDelimiter;
    orderedListLevel = previousLevel;

    writer.line();
  }

  @Override
  public void visit(ListItem li) {
    if (orderedListCounter != null) {
      writer.write(StringUtils.repeat(INDENT, orderedListLevel) + String.valueOf(orderedListCounter) +
          orderedListDelimiter + " ");
      visitChildren(li);
      writer.line();
      orderedListCounter++;
    } else if (bulletListMarker != null) {
      writer.write( StringUtils.repeat(INDENT, bulletListLevel) + bulletListMarker + " ");
      visitChildren(li);
      writer.line();
    }
  }

  @Override
  public void visit(FencedCodeBlock code) {
    writer.line();
    writer.writeStripped(StringUtils.repeat(code.getFenceChar(), code.getFenceLength()));
    if (code.getInfo() != null) {
      writer.write(code.getInfo());
    }
    writer.line();
    visitChildren(code, Collections.<Class<? extends Node>>singleton(Text.class));
    writer.line();
    writer.writeStripped(StringUtils.repeat(code.getFenceChar(), code.getFenceLength()));
    writer.line();
  }

  @Override
  public void visit(CustomNode node) {
    if (node instanceof KeywordNode) {
      visit((KeywordNode) node);
    } else if (node instanceof EmojiNode) {
      visit((EmojiNode) node);
    } else if (node instanceof MentionNode) {
      visit((MentionNode) node);
    } else if (node instanceof TagNode) {
      visit(TagNode.class.cast(node));
    } else if (node instanceof DateTimeNode) {
      visit((DateTimeNode) node);
    }
  }

  @Override
  public void visit(CustomBlock node) {
    if (node instanceof TableNode) {
      visit((TableNode) node);
    } else if (node instanceof TableRowNode) {
      visit((TableRowNode) node);
    } else if (node instanceof TableCellNode) {
      visit((TableCellNode) node);
    } else if (node instanceof PreformattedNode) {
      visit((PreformattedNode) node);
    } else if (node instanceof FormNode) {
      visit((FormNode) node);
    } else if (node instanceof SelectNode) {
      visit((SelectNode) node);
    } else if (node instanceof OptionNode) {
      visit((OptionNode) node);
    } else if (node instanceof ButtonNode) {
      visit((ButtonNode) node);
    } else if (node instanceof TextFieldNode) {
      visit((TextFieldNode) node);
    } else if (node instanceof TextAreaNode) {
      visit((TextAreaNode) node);
    } else if (node instanceof PersonSelectorNode) {
      visit((PersonSelectorNode) node);
    } else if (node instanceof FormElementNode) {
      visit((FormElementNode) node);
    }
  }

  private void visit(EmojiNode emoji){
    writer.write(emoji.getOpeningDelimiter());
    writer.write(emoji.getShortcode());
    writer.write(emoji.getClosingDelimiter());
  }

  private void visit(FormElementNode formElement) {
    writer.write(formElement.getOpeningDelimiter());
    writer.write(addEscapeCharacter(formElement.getText()));
    writer.write(formElement.getClosingDelimiter());
  }

  private void visit(FormNode form) {
    writer.write(form.getOpeningDelimiter());
    visitChildren(form);
    writer.write(form.getClosingDelimiter());
  }

  private void visit(ButtonNode button) {
    writer.write(button.getOpeningDelimiter());
    visitChildren(button);
    writer.write(button.getClosingDelimiter());
  }

  private void visit(SelectNode select) {
    writer.write(select.getOpeningDelimiter());
    writer.write(addEscapeCharacter(select.getText()));
    writer.write(select.getClosingDelimiter());
    visitChildren(select);
  }

  private void visit(OptionNode option) {
    writer.write(option.getOpeningDelimiter());
    visitChildren(option);
    writer.write(option.getClosingDelimiter());
  }
  
  private void visit(TextFieldNode textField) {
    writer.write(textField.getOpeningDelimiter());
    writer.write(addEscapeCharacter(textField.getText()));
    writer.write(textField.getClosingDelimiter());
  }

  private void visit(TextAreaNode textArea) {
    writer.write(textArea.getOpeningDelimiter());
    writer.write(addEscapeCharacter(textArea.getText()));
    writer.write(textArea.getClosingDelimiter());
  }

  private void visit(PersonSelectorNode personSelector) {
    writer.write(personSelector.getOpeningDelimiter());
    writer.write(addEscapeCharacter(personSelector.getText()));
    writer.write(personSelector.getClosingDelimiter());
  }
  
  private void visit(KeywordNode keyword) {
    String text = keyword.getPrefix() + keyword.getText();

    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    node.put(ID, text);
    node.put(TEXT, text);
    node.put(INDEX_START, writer.length());
    node.put(INDEX_END, writer.length() + text.length());
    node.put(TYPE, "KEYWORD");
    putJsonObject(HASHTAGS, node);

    writer.write(text);
  }

  private void visit(MentionNode mention) {
    String text = mention.getText();

    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    node.put(ID, mention.getUid());
    node.put(SCREEN_NAME, mention.getScreenName());
    node.put(PRETTY_NAME, mention.getPrettyName());
    node.put(TEXT, text);
    node.put(INDEX_START, writer.length());
    node.put(INDEX_END, writer.length() + text.length());
    node.put(USER_TYPE, "lc");
    node.put(TYPE, "USER_FOLLOW");
    putJsonObject(USER_MENTIONS, node);

    writer.write(text);
  }
  private void visit(TableNode table) {
    writer.write(table.getOpeningDelimiter());
    visitChildren(table);
    writer.write(table.getClosingDelimiter());
  }

  private void visit(TableRowNode row) {
    visitChildren(row);
    if (row.getNext() != null) {
      writer.write(row.getDelimiter());
    }
  }

  private void visit(TableCellNode cell) {
    visitChildren(cell);
    if (cell.getNext() != null) {
      writer.write(cell.getDelimiter());
    }
  }

  private void visit(PreformattedNode pre) {
    this.removeNewlines = false;
    writer.write(pre.getOpeningDelimiter());
    visitChildren(pre);
    writer.write(pre.getClosingDelimiter());
    writer.line();
    this.removeNewlines = true;
  }

  private void visit(TagNode tag) {
    String text = tag.getPrefix() + tag.getText();
    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    node.put(ID, text);
    node.put(TEXT, text);
    node.put(INDEX_START, writer.length());
    node.put(INDEX_END, writer.length() + text.length());
    node.put(TYPE, "KEYWORD");
    if (tag.getData() != null) {
      node.set(DATA, tag.getData());
    }
    putJsonObject(HASHTAGS, node);
    writer.write(text);
  }

  private void visit(DateTimeNode dateTimeNode) {
    String text = dateTimeNode.getValue();
    ObjectNode node = new ObjectNode(JsonNodeFactory.instance);
    node.put(ID, dateTimeNode.getEntityId());
    node.put(TEXT, text);
    node.put(INDEX_START, writer.length());
    node.put(INDEX_END, writer.length() + text.length());
    node.put(TYPE, "DATE_TIME");
    node.put(VALUE, dateTimeNode.getValue());
    if (dateTimeNode.getFormat() != null) {
      node.put(FORMAT, dateTimeNode.getFormat());
    }
    putJsonObject(DATETIMES, node);
    writer.write(text);
  }

  private void visitDelimited(Delimited delimited) {
    writer.write(delimited.getOpeningDelimiter());
    visitChildren((Node) delimited);
    writer.write(delimited.getClosingDelimiter());
  }

  /**
   * Recursively visit the children of the node, processing only those specified by the parameter "includeNodes".
   */
  private void visitChildren(Node parent, Collection<Class<? extends Node>> includeNodes) {
    Node child = parent.getFirstChild();
    while (child != null) {
      // A subclass of this visitor might modify the node, resulting in getNext returning a different node or no
      // node after visiting it. So get the next node before visiting.
      Node next = child.getNext();
      if (includeNodes.contains(child.getClass())) {
        child.accept(this);
      } else {
        visitChildren(child, includeNodes);
      }
      child = next;
    }
  }

  public static String addEscapeCharacter(String content) {
    // If the line consists only of repeated reserved Markdown characters, ignore it
    Matcher matcher = NOESCAPE_PATTERN.matcher(content);
    if (!matcher.matches()) {
      // Otherwise, escape reserved Markdown characters in text nodes to prevent them from being interpreted as MD
      content = StringUtils.replaceEach(content, new String[]{"_","*","-","+","`"}, new String[]{"\\_","\\*","\\-","\\+","\\`"});
    }
    return content;
  }

  private void putJsonObject(String field, JsonNode value) {
    if (!json.has(field)) {
      json.set(field, new ArrayNode(JsonNodeFactory.instance));
    }

    ((ArrayNode) json.get(field)).add(value);
  }

  /**
   * Get the text representation of the input document.
   * @return Markdown text
   */
  public String getText() {
    return writer.toString();
  }

  /**
   * Get the JSON representation of the input document.
   * @return JSON object containing entries for keywords (hashtags and cashtags), user mentions and URLs in the
   * input documents.
   */
  public ObjectNode getJson() {
    return json;
  }

  class TrackingWriter extends TextContentWriter {

    final StringBuilder out;

    TrackingWriter(StringBuilder out) {
      super(out);
      this.out = out;
    }

    int length() {
      return out.length();
    }

    char getLastChar() {
      int length = length();
      return (length != 0) ? out.charAt(length - 1) : 0;
    }

    void doubleLine() {
      char lastChar = getLastChar();
      if (lastChar != 0 && lastChar != '\n') {
        write("\n\n");
      }
    }

    @Override
    public String toString() {
      return out.toString();
    }
  }
}

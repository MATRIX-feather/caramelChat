package moe.caramel.chat.wrapper;

import moe.caramel.chat.util.Rect;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField.StringView;

/**
 * MultiLineEditBox Component Wrapper
 */
public final class WrapperMultilineEditBox extends AbstractIMEWrapper {

    private final MultiLineEditBox wrapped;
    public boolean valueChanged;

    public WrapperMultilineEditBox(final MultiLineEditBox textField) {
        super(textField.getValue());
        this.wrapped = textField;
    }

    @Override
    protected void insert(final String text) {
        if (this.editable()) {
            this.wrapped.textField.insertText(text);
        }
    }

    @Override
    protected int getCursorPos() {
        return wrapped.textField.cursor;
    }

    @Override
    protected int getHighlightPos() {
        return wrapped.textField.selectCursor;
    }

    @Override
    public boolean blockTyping() {
        return wrapped.textField.overflowsLineLimit("");
    }

    @Override
    protected String getTextWithPreview() {
        return wrapped.getValue();
    }

    @Override
    protected void setPreviewText(final String text) {
        this.valueChanged = true;
        this.wrapped.textField.value = text;
        this.wrapped.textField.reflowDisplayLines();
    }

    @Override
    public Rect getRect() {
        // Ignore
        if (this.getStatus() == InputStatus.NONE) {
            return Rect.EMPTY;
        }

        // Calculate rect position
        final int editEndPos = this.getSecondStartPos();

        int index;
        StringView stringView = null;
        for (index = 0; index < this.wrapped.textField.displayLines.size(); index++) {
            stringView = this.wrapped.textField.displayLines.get(index);
            if (editEndPos >= stringView.beginIndex() &&
                editEndPos <= stringView.endIndex()) {
                break;
            }
        }

        final int xWidth;
        final int yHeight;
        if (stringView == null) {
            xWidth = 0;
            yHeight = this.wrapped.textField.getLineCount();
        } else {
            final String result = this.getTextWithPreview().substring(stringView.beginIndex(), this.getSecondStartPos());
            xWidth = this.wrapped.font.width(result);
            yHeight = index + 1;
        }

        final float x = wrapped.getInnerLeft() + (xWidth);
        final float y = wrapped.getInnerTop() + (yHeight * wrapped.font.lineHeight);
        return new Rect(x, y, wrapped.getWidth(), wrapped.getHeight());
    }
}

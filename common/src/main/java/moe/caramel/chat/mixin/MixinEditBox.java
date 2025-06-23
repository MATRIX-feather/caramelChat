package moe.caramel.chat.mixin;

import moe.caramel.chat.IHavePreeditText;
import moe.caramel.chat.controller.EditBoxController;
import moe.caramel.chat.wrapper.AbstractIMEWrapper;
import moe.caramel.chat.wrapper.WrapperEditBox;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * EditBox Component Mixin
 */
@Mixin(value = EditBox.class, priority = 0)
public abstract class MixinEditBox implements EditBoxController, IHavePreeditText
{

    @Unique private WrapperEditBox caramelChat$wrapper;
    @Unique private BiFunction<String, Integer, FormattedCharSequence> caramelChat$formatter;
    @Unique private int caramelChat$cacheCursorPos, caramelChat$cacheHighlightPos;
    @Shadow private BiFunction<String, Integer, FormattedCharSequence> formatter;
    @Shadow private boolean canLoseFocus;
    @Shadow public int highlightPos;
    @Shadow public int cursorPos;
    @Shadow public String value;

    @Shadow @Final public Font font;

    @Shadow private int textColor;

    @Unique private FocusableTextWidget caramelChat$textWidget;

    @Redirect(
        method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/network/chat/Component;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setValue(Ljava/lang/String;)V")
    )
    private void init(final EditBox self, final String value) {
        this.caramelChat$wrapper = new WrapperEditBox((EditBox) (Object) this);
        this.caramelChat$textWidget = new FocusableTextWidget(4096, Component.literal("The quick brown fox jumped over the lazy dog."), font);
        self.setValue(value);
    }

    @Inject(
        method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/components/EditBox;Lnet/minecraft/network/chat/Component;)V",
        at = @At("TAIL")
    )
    private void lazyInit(final CallbackInfo ci) {
        if (this.caramelChat$wrapper == null) {
            this.caramelChat$wrapper = new WrapperEditBox((EditBox) (Object) this);
        }

        if (caramelChat$textWidget == null) {
            this.caramelChat$textWidget = new FocusableTextWidget(4096, Component.literal("The quick brown fox jumped over the lazy dog."), font);
        }

        this.caramelChat$formatter = this.formatter; // Cache
        //this.caramelChat$caretFormatter();
    }

    @Override
    public WrapperEditBox caramelChat$wrapper() {
        return caramelChat$wrapper;
    }

    // ================================ (Formatter)

    @Inject(method = "setFormatter", at = @At("TAIL"))
    private void setFormatter(final BiFunction<String, Integer, FormattedCharSequence> formatter, final CallbackInfo ci) {
        this.caramelChat$formatter = formatter; // Cache
        //this.caramelChat$caretFormatter();
    }

    @Unique
    private void caramelChat$caretFormatter() {
        // Set caret renderer
        this.formatter = ((original, firstPos) -> {
            /* Original */
            if (caramelChat$wrapper.getStatus() == AbstractIMEWrapper.InputStatus.NONE) {
                return caramelChat$formatter.apply(original, firstPos);
            }
            /* Warning */
            else if (caramelChat$wrapper.blockTyping()) {
                return FormattedCharSequence.forward(original, Style.EMPTY.withColor(ChatFormatting.RED));
            }
            /* Custom */
            else {
                // Check Position
                // Empty
                // FirstPos ex. [ ABCD|EFG}(INPUT)HIJK ]
                // LastPos ex. [ ABCDEFG(INPUT)HI|JK} ]
                final int lastPos = (firstPos + original.length()); // firstPos ~ lastPos
                if (lastPos <= caramelChat$wrapper.getFirstEndPos() || caramelChat$wrapper.getSecondStartPos() < firstPos) {
                    return caramelChat$formatter.apply(original, firstPos);
                }

                // Process
                final int firstLen = (caramelChat$wrapper.getFirstEndPos() - firstPos);
                final int previewLen = (caramelChat$wrapper.getSecondStartPos() - caramelChat$wrapper.getFirstEndPos());
                final int inputEndPoint = Math.min(original.length(), (firstLen + previewLen));

                final List<FormattedCharSequence> list = new ArrayList<>();
                final String first = original.substring(0, firstLen);
                final String input = original.substring(firstLen, inputEndPoint);
                final String second = original.substring(inputEndPoint);
                list.add(FormattedCharSequence.forward(first, Style.EMPTY));
                list.add(FormattedCharSequence.forward(input, Style.EMPTY.withUnderlined(true)));
                list.add(FormattedCharSequence.forward(second, Style.EMPTY));

                return FormattedCharSequence.composite(list);
            }
        });
    }

    // ================================ (IME)

    @Inject(method = "setValue", at = @At("HEAD"))
    private void setValueHead(final String text, final CallbackInfo ci) {
        // setStatusToNone -> forceUpdateOrigin -> onValueChange
        if (this.caramelChat$wrapper != null && this.caramelChat$wrapper.valueChanged) {
            this.caramelChat$cacheCursorPos = 0;
            this.caramelChat$cacheHighlightPos = 0;
        } else {
            this.caramelChat$setStatusToNone();
            this.caramelChat$forceUpdateOrigin(null);
        }
    }

    @Redirect(
        method = "setValue",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"
        )
    )
    private boolean setValuePredicateTest(final Predicate<String> predicate, final Object value) {
        if (this.caramelChat$wrapper != null && this.caramelChat$wrapper.valueChanged) {
            this.caramelChat$cacheCursorPos = this.cursorPos;
            this.caramelChat$cacheHighlightPos = this.highlightPos;
            return true;
        }

        return predicate.test((String) value);
    }

    @Inject(
        method = "setValue",
        at = @At(
            value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/client/gui/components/EditBox;moveCursorToEnd(Z)V"
        )
    )
    private void setValueInvoke(final String finalText, final CallbackInfo ci) {
        this.caramelChat$forceUpdateOrigin(finalText);
        this.caramelChat$setPreview(null);
    }

    @Inject(method = "insertText", at = @At("HEAD"))
    private void insertTextHead(final String text, final CallbackInfo ci) {
        // setStatusToNone -> forceUpdateOrigin -> onValueChange
        this.caramelChat$setStatusToNone();
    }

    @Inject(
        method = "insertText",
        at = @At(
            value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/client/gui/components/EditBox;onValueChange(Ljava/lang/String;)V"
        )
    )
    private void insertTextInvoke(final String textToWrite, final CallbackInfo ci) {
        this.caramelChat$forceUpdateOrigin(this.value);
    }

    @Inject(
        method = "deleteCharsToPos",
        at = @At(
            value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/client/gui/components/EditBox;moveCursorTo(IZ)V"
        )
    )
    private void deleteChars(final int pos, final CallbackInfo ci) {
        this.caramelChat$wrapper.setOrigin(this.value);
    }

    @Inject(method = "setFocused", at = @At("TAIL"))
    private void setFocused(final boolean focused, final CallbackInfo ci) {
        if (this.caramelChat$wrapper != null) {
            this.caramelChat$wrapper.setFocused(focused || !this.canLoseFocus);
        }
    }

    @Inject(method = "setCanLoseFocus", at = @At("HEAD"))
    private void setCanLoseFocus(final boolean canLoseFocus, final CallbackInfo ci) {
        if (this.caramelChat$wrapper != null && !canLoseFocus) {
            this.caramelChat$wrapper.setFocused(true);
        }
    }

    @Unique
    private void caramelChat$setStatusToNone() {
        if (this.caramelChat$wrapper != null) {
            this.caramelChat$wrapper.setToNoneStatus();
        }
    }

    @Unique
    private void caramelChat$forceUpdateOrigin(final String text) {
        if (this.caramelChat$wrapper != null) {
            this.caramelChat$wrapper.setOrigin(text);
        }
    }

    // =============================== [RENDER]
    @Nullable
    @Unique
    private String caramelChat$preeditString = "";

    @Override
    public void caramelChat$setPreview(@Nullable String text)
    {
        this.caramelChat$preeditString = text;

        if (text != null)
            this.caramelChat$textWidget.setMessage(Component.literal(text));
    }

    @Override
    public String caramelChat$getPreview()
    {
        return caramelChat$preeditString;
    }

    @Inject(
            method = "renderWidget",
            at = @At(value = "HEAD")
    )
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    {
        this.caramelChat$drawPreedit(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Unique
    private void caramelChat$drawPreedit(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        if (caramelChat$preeditString == null || caramelChat$preeditString.isBlank())
            return;

        var asEditBox = (EditBox)(Object)this;

        // The padding of the FocusableTextWidget
        int padding = 4;

        int height = font.lineHeight;

        int startY = asEditBox.getY() - height - padding - 2;

        // Move down if we reached out of the screen
        if (startY < 0)
            startY = asEditBox.getY();

        caramelChat$textWidget.setX(asEditBox.getX() + padding);
        caramelChat$textWidget.setY(startY);
        caramelChat$textWidget.setHeight(font.lineHeight);
        caramelChat$textWidget.setWidth(font.width(caramelChat$textWidget.getMessage()));

        caramelChat$textWidget.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}

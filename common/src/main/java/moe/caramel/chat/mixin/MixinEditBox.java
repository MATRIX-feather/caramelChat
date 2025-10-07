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
    }

    @Override
    public WrapperEditBox caramelChat$wrapper() {
        return caramelChat$wrapper;
    }

    // ================================ (IME)

    @Inject(method = "setValue", at = @At("HEAD"))
    private void setValueHead(final String text, final CallbackInfo ci) {
        // setStatusToNone -> forceUpdateOrigin -> onValueChange
        if (this.caramelChat$wrapper != null && this.caramelChat$wrapper.valueChanged) {
            //todo: Remove this
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

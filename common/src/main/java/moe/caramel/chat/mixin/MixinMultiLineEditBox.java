package moe.caramel.chat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import moe.caramel.chat.IHavePreeditText;
import moe.caramel.chat.wrapper.AbstractIMEWrapper;
import moe.caramel.chat.wrapper.AbstractIMEWrapper.InputStatus;
import moe.caramel.chat.wrapper.WrapperMultilineEditBox;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import java.util.function.Consumer;

/**
 * MultiLineEditBox Component Mixin
 */
@Mixin(MultiLineEditBox.class)
public final class MixinMultiLineEditBox implements IHavePreeditText
{

    @Unique private WrapperMultilineEditBox caramelChat$wrapper;
    @Unique private int caramelChat$viewBeginPos = -1, caramelChat$viewEndPos = -1;
    @Shadow @Final public MultilineTextField textField;

    @Shadow @Final public Font font;

    @Unique private FocusableTextWidget caramelChat$textWidget;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(final CallbackInfo ci) {
        this.caramelChat$wrapper = new WrapperMultilineEditBox((MultiLineEditBox) (Object) this);
        this.caramelChat$replaceValueListener(this.textField.valueListener);
        this.caramelChat$textWidget = new FocusableTextWidget(1024, Component.literal("The quick brown fox jumped over the lazy dog."), font);
    }

    // ================================ (Formatter)
/*
    @ModifyArgs(
        method = "renderContents",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/String;substring(II)Ljava/lang/String;",
            ordinal = 1
        )
    )
    private void captureLineRenderPositionsMiddle(final Args args) {
        this.captureLineRenderPositionsEnd(args);
    }

    @ModifyArgs(
        method = "renderContents",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/String;substring(II)Ljava/lang/String;",
            ordinal = 2
        )
    )
    private void captureLineRenderPositionsEnd(final Args args) {
        this.caramelChat$viewBeginPos = args.get(0);
        this.caramelChat$viewEndPos = args.get(1);
    }

    @WrapOperation(
        method = "renderContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
            ordinal = 1
        )
    )
    private void renderCaretMiddle(final GuiGraphics instance, final Font font, final String text, final int x, final int y, final int color, final boolean dropShadow, final Operation<Integer> original) {
        this.renderCaretEnd(instance, font, text, x, y, color, dropShadow, original);
    }

    @WrapOperation(
        method = "renderContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
            ordinal = 2
        )
    )
    private void renderCaretEnd(final GuiGraphics instance, final Font font, final String text, final int x, final int y, final int color, final boolean dropShadow, final Operation<Integer> original) {
        // Check IME Status
        if (text.isEmpty() || caramelChat$wrapper.getStatus() == AbstractIMEWrapper.InputStatus.NONE) {
            original.call(instance, font, text, x, y, color, dropShadow);
            return;
        }

        // Render Caret
        final int firstEnd = caramelChat$wrapper.getFirstEndPos();
        final int secondStart = caramelChat$wrapper.getSecondStartPos();

        if (firstEnd < this.caramelChat$viewEndPos && this.caramelChat$viewBeginPos < secondStart) {
            final int localStart = Math.max(0, firstEnd - this.caramelChat$viewBeginPos);
            final int localEnd = Math.min(text.length(), secondStart - this.caramelChat$viewBeginPos);

            if (localStart >= localEnd) {
                original.call(instance, font, text, x, y, color, dropShadow);
                return;
            }

            final String before = text.substring(0, localStart);
            final String underlined = text.substring(localStart, localEnd);
            final String after = text.substring(localEnd);

            final String result = before + ChatFormatting.UNDERLINE + underlined + ChatFormatting.RESET + after;
            original.call(instance, font, result, x, y, color, dropShadow);
            return;
        }

        // No need to render caret
        original.call(instance, font, text, x, y, color, dropShadow);
    }
*/
    // ================================ (IME)

    //region feather: preedit rendering

    @Nullable
    @Unique
    private String caramelChat$preeditString;

    @Override
    public String caramelChat$getPreview()
    {
        return caramelChat$preeditString == null ? "" : caramelChat$preeditString;
    }

    @Override
    public void caramelChat$setPreview(@Nullable String text)
    {
        caramelChat$preeditString = text;
    }

    @Inject(
            method = "renderContents",
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

        var asEditBox = (MultiLineEditBox)(Object)this;

        // The padding of the FocusableTextWidget
        int padding = 4;

        int height = font.lineHeight;

        int startY = asEditBox.getY() - height - padding - 2;

        // Move down if we reached out of the screen
        if (startY < 0)
            startY = 0;

        caramelChat$textWidget.setX(asEditBox.getX() + padding);
        caramelChat$textWidget.setY(startY);
        caramelChat$textWidget.setHeight(font.lineHeight);
        caramelChat$textWidget.setWidth(font.width(caramelChat$textWidget.getMessage()));

        caramelChat$textWidget.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    //endregion feather: preedit rendering

    @Inject(method = "setValueListener", at = @At("TAIL"), cancellable = true)
    private void setValueListener(final Consumer<String> valueListener, final CallbackInfo ci) {
        ci.cancel();
        this.caramelChat$replaceValueListener(valueListener);
    }

    @Inject(method = "seekCursorScreen", at = @At("TAIL"))
    private void seekCursorScreen(final double mouseX, final double mouseY, final CallbackInfo ci) {
        if (this.caramelChat$wrapper != null) {
            this.caramelChat$wrapper.setOrigin();
            this.caramelChat$wrapper.setToNoneStatus();
        }
    }

    @Inject(method = "setFocused", at = @At("TAIL"))
    private void setFocused(final boolean focused, final CallbackInfo ci) {
        if (this.caramelChat$wrapper != null) {
            this.caramelChat$wrapper.setFocused(focused);
        }
    }

    @Unique
    private void caramelChat$replaceValueListener(final Consumer<String> valueListener) {
        this.textField.setValueListener((value) -> {
            if (this.caramelChat$wrapper == null || this.caramelChat$wrapper.getStatus() == InputStatus.NONE) {
                valueListener.accept(value);
                this.caramelChat$wrapper.setOrigin(value);
            }
        });
    }
}

package moe.caramel.chat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import moe.caramel.chat.IHavePreeditText;
import moe.caramel.chat.controller.ScreenController;
import moe.caramel.chat.wrapper.AbstractIMEWrapper;
import moe.caramel.chat.wrapper.WrapperSignEditScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Consumer;

/**
 * SignEdit Screen Mixin
 */
@Mixin(value = AbstractSignEditScreen.class, priority = 0)
public final class MixinSignEditScreen implements ScreenController, IHavePreeditText {

    @Unique private WrapperSignEditScreen caramelChat$wrapper;
    @Unique private boolean caramelChat$lazyInit;
    @Shadow @Nullable public TextFieldHelper signField;
    @Shadow @Final public SignBlockEntity sign;
    @Shadow public int line;

    @Unique
    private void caramelChat$tryInitTextWidget()
    {
        var asSignEditScreen = (AbstractSignEditScreen)(Object)this;
        if (caramelChat$textWidget != null)
        {
            caramelChat$textWidget.setX(asSignEditScreen.width / 2);
            return;
        }

        var font = asSignEditScreen.font;

        this.caramelChat$textWidget = new FocusableTextWidget(4096, Component.literal("The quick brown fox jumped over the lazy dog."), font);
        caramelChat$textWidget.setCentered(true);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void init(final CallbackInfo ci) {
        this.caramelChat$wrapper = new WrapperSignEditScreen((AbstractSignEditScreen) (Object) this);
        this.caramelChat$wrapper.setOrigin();
        caramelChat$tryInitTextWidget();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void lazyInit(final CallbackInfo ci) {
        // Stendhal mod creates a new signField... :scream:
        if (!caramelChat$lazyInit && signField != null) {
            this.caramelChat$lazyInit = true;
            caramelChat$tryInitTextWidget();

            final Consumer<String> previous = (signField.setMessageFn);
            this.signField.setMessageFn = (value) -> {
                previous.accept(value);
                this.caramelChat$wrapper.setOrigin();
            };
        }
    }

    @Inject(method = "keyPressed", at = {
        @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/font/TextFieldHelper;setCursorToEnd()V"),
        @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/client/gui/font/TextFieldHelper;setCursorToEnd()V")
    })
    private void keyPressed(final int key, final int scancode, final int action, final CallbackInfoReturnable<Boolean> cir) {
        this.caramelChat$wrapper.setOrigin();
    }

    @Redirect(
        method = "keyPressed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/font/TextFieldHelper;keyPressed(I)Z"
        )
    )
    private boolean helperKeyPressed(final TextFieldHelper helper, final int key) {
        final boolean result = helper.keyPressed(key);
        if (result) {
            this.caramelChat$wrapper.setToNoneStatus();
        }
        return result;
    }

    @Unique
    private FocusableTextWidget caramelChat$textWidget;

    @Unique
    @Nullable
    private String caramelChat$preeditString = "";

    @Override
    public void caramelChat$setPreview(@Nullable String text)
    {
        caramelChat$preeditString = text;

        if (caramelChat$textWidget != null) {
            if (text != null && !text.isBlank())
                caramelChat$textWidget.setMessage(Component.literal(text));
            else
                caramelChat$textWidget.setMessage(Component.empty());
        }
    }

    @Override
    public String caramelChat$getPreedit()
    {
        return caramelChat$preeditString;
    }

    @Inject(
            method = "render",
            at = @At(value = "TAIL")
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

        var asSignEditScreen = (AbstractSignEditScreen)(Object)this;

        // The padding of the FocusableTextWidget
        int padding = 4;
        var font = asSignEditScreen.font;

        int height = font.lineHeight;

        int startY = 0;

        // Move down if we reached out of the screen
        //if (startY < 0)
        //    startY = asEditBox.getY();

        caramelChat$textWidget.setX(0 + padding);
        caramelChat$textWidget.setY(startY);
        caramelChat$textWidget.setHeight(font.lineHeight);
        caramelChat$textWidget.setWidth(font.width(caramelChat$textWidget.getMessage()));

        caramelChat$textWidget.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}

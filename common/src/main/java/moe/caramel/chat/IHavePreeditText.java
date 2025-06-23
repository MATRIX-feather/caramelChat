package moe.caramel.chat;

import org.jetbrains.annotations.Nullable;

public interface IHavePreeditText
{
    public void caramelChat$setPreview(@Nullable String text);
    public String caramelChat$getPreview();
}

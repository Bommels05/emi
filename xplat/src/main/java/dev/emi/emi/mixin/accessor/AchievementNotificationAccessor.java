package dev.emi.emi.mixin.accessor;

import net.minecraft.client.gui.AchievementNotification;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AchievementNotification.class)
public interface AchievementNotificationAccessor {

    @Accessor("permanent")
    void setPermanent(boolean permanent);

    @Accessor("time")
    void setTime(long time);

    @Accessor("time")
    long getTime();

}

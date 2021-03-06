package com.brandon3055.brandonscore.client.gui.modulargui.guielements;

import com.brandon3055.brandonscore.client.BCClientEventHandler;
import com.brandon3055.brandonscore.client.gui.modulargui.GuiElement;
import com.brandon3055.brandonscore.utils.LogHelperBC;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.tileentity.SkullTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * Created by brandon3055 on 23/10/2016.
 */
public class GuiEntityRenderer extends GuiElement<GuiEntityRenderer> {
    private static Map<ResourceLocation, Entity> entityCache = new HashMap<>();
    private static List<ResourceLocation> invalidEntities = new ArrayList<>();

    private float rotationSpeed = 1;
    private float lockedRotation = 0;
    private Entity entity;
    private ResourceLocation entityName;
    private boolean invalidEntity = false;
    private boolean rotationLocked = false;
    private boolean trackMouse = false;
    private boolean drawName = false;
    //    private boolean animate = false;
    public boolean silentErrors = false;
    public boolean force2dSize = false;

    public GuiEntityRenderer() {
    }

    public GuiEntityRenderer(int xPos, int yPos) {
        super(xPos, yPos);
    }

    public GuiEntityRenderer(int xPos, int yPos, int xSize, int ySize) {
        super(xPos, yPos, xSize, ySize);
    }

    public GuiEntityRenderer setEntity(Entity entity) {
        this.entity = entity;
        if (this.entity == null) {
            if (!silentErrors) {
                LogHelperBC.dev("GuiEntityRenderer#setEntity: Invalid Entity - " + entityName);
            }
            invalidEntity = true;
            return this;
        }

        this.entityName = entity.getType().getRegistryName();
        invalidEntity = false;

        if (invalidEntities.contains(entityName)) {
            invalidEntity = true;
        }

        return this;
    }

    public GuiEntityRenderer setEntity(ResourceLocation entity) {
        this.entityName = entity;
        this.entity = entityCache.computeIfAbsent(entity, resourceLocation -> {
            EntityType type = ForgeRegistries.ENTITIES.getValue(entity);
            return type == null ? null : type.create(mc.world);
        });
        invalidEntity = false;

        if (this.entity == null) {
            if (!silentErrors) {
                LogHelperBC.dev("GuiEntityRenderer#setEntity: Invalid Entity - " + entityName);
            }
            invalidEntity = true;
        }

        if (invalidEntities.contains(entityName)) {
            invalidEntity = true;
        }

        return this;
    }

    public GuiEntityRenderer setSilentErrors(boolean silentErrors) {
        this.silentErrors = silentErrors;
        return this;
    }

    public GuiEntityRenderer setForce2dSize(boolean force2dSize) {
        this.force2dSize = force2dSize;
        return this;
    }

    @Override
    public void renderElement(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        super.renderElement(minecraft, mouseX, mouseY, partialTicks);

        if (invalidEntity && !silentErrors) {
            LogHelperBC.dev("GuiEntityRenderer: Invalid Entity - " + entityName);
            return;
        }

        try {
            if (entity != null) {
                Rectangle rect = getInsetRect();
                float scale = (float) (force2dSize ? (Math.min(rect.height / entity.getHeight(), rect.width / entity.getWidth())) : rect.height / entity.getHeight());

                double zLevel = getRenderZLevel() + 100;
                double posX = rect.x + (rect.width / 2D);

                float rotation = isRotationLocked() ? getLockedRotation() : (BCClientEventHandler.elapsedTicks + partialTicks) * getRotationSpeedMultiplier();
                if (entity instanceof LivingEntity) {
                    int eyeOffset = (int) ((entity.getHeight() - entity.getEyeHeight()) * scale);
                    drawEntityOnScreen(posX, rect.y, scale, (int) posX - mouseX, rect.y - mouseY + eyeOffset, (LivingEntity) entity, trackMouse, rotation, drawName, zLevel);
                } else {
                    drawEntityOnScreen(posX, rect.y, scale, entity, rotation, zLevel);
                }
            }
        }
        catch (Throwable e) {
            invalidEntity = true;
            invalidEntities.add(entityName);
            LogHelperBC.error("Failed to build entity in GUI. This is not a bug there are just some entities that can not be rendered like this.");
            LogHelperBC.error("Entity: " + entity);
            e.printStackTrace();
        }
    }

    public boolean isRotationLocked() {
        return rotationLocked;
    }

    public GuiEntityRenderer rotationLocked(boolean rotationLocked) {
        this.rotationLocked = rotationLocked;
        return this;
    }

    public GuiEntityRenderer setLockedRotation(float lockedRotation) {
        this.lockedRotation = lockedRotation;
        rotationLocked(true);
        return this;
    }

    public float getLockedRotation() {
        return lockedRotation;
    }

    public boolean isInvalidEntity() {
        return invalidEntity;
    }

    public GuiEntityRenderer setRotationSpeedMultiplier(float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
        return this;
    }

    public GuiEntityRenderer setTrackMouse(boolean trackMouse) {
        this.trackMouse = trackMouse;
        return this;
    }

    public GuiEntityRenderer setDrawName(boolean drawName) {
        this.drawName = drawName;
        return this;
    }

//    public GuiEntityRenderer setAnimate(boolean animate) {
//        this.animate = animate;
//        return this;
//    }

    public float getRotationSpeedMultiplier() {
        return rotationSpeed;
    }

    //TODO This no needs to be re written
    public static void drawEntityOnScreen(double posX, double posY, double scale, Entity ent, double rotation, double zOffset) {
        RenderSystem.enableColorMaterial();
        RenderSystem.pushMatrix();

//        RenderSystem.translate((float) posX, (float) posY, 50.0F);
        RenderSystem.translated((float) posX, (float) posY + (ent.getHeight() * scale), zOffset);

        RenderSystem.scalef((float) (-scale), (float) scale, (float) scale);
        RenderSystem.rotatef(180.0F, 0.0F, 0.0F, 1.0F);
        float f1 = ent.rotationYaw;
        float f2 = ent.rotationPitch;
        RenderSystem.rotatef(135.0F + (float) rotation, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        RenderSystem.rotatef(-135.0F, 0.0F, 1.0F, 0.0F);
//        RenderSystem.rotate(-((float) Math.atan((double) (mouseY / 40.0F))) * 20.0F, 1.0F, 0.0F, 0.0F);
//        ent.rotationYaw = (float) Math.atan((double) (mouseX / 40.0F)) * 40.0F;
//        ent.rotationPitch = -((float) Math.atan((double) (mouseY / 40.0F))) * 20.0F;
        RenderSystem.translatef(0.0F, 0.0F, 0.0F);
        EntityRendererManager rendermanager = Minecraft.getInstance().getRenderManager();
//        rendermanager.setPlayerViewY(180.0F);
//        rendermanager.setRenderShadow(false);
//        rendermanager.renderEntity(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
//        rendermanager.setRenderShadow(true);

        MatrixStack matrixstack = new MatrixStack();
        EntityRendererManager entityrenderermanager = Minecraft.getInstance().getRenderManager();
//        quaternion1.conjugate();
//        entityrenderermanager.setCameraOrientation(quaternion1);
        entityrenderermanager.setRenderShadow(false);
        IRenderTypeBuffer.Impl irendertypebuffer$impl = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        entityrenderermanager.renderEntityStatic(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrixstack, irendertypebuffer$impl, 15728880);
        irendertypebuffer$impl.finish();
        entityrenderermanager.setRenderShadow(true);



        ent.rotationYaw = f1;
        ent.rotationPitch = f2;
        RenderSystem.popMatrix();
        RenderHelper.disableStandardItemLighting();
        RenderSystem.disableRescaleNormal();
//        RenderSystem.activeTexture(GLX.GL_TEXTURE1);
//        RenderSystem.disableTexture();
//        RenderSystem.activeTexture(GLX.GL_TEXTURE0);
    }

    //TODO so does this
    public static void drawEntityOnScreen(double posX, double posY, double scale, double mouseX, double mouseY, LivingEntity ent, boolean trackMouse, double noTrackRotation, boolean drawName, double zOffset) {
        float rotation = trackMouse ? 0 : (float) noTrackRotation;
        if (!trackMouse) {
            mouseX = 0;
            mouseY = 0;
        }

        RenderSystem.enableColorMaterial();
        RenderSystem.pushMatrix();
//        RenderSystem.translate((float) posX, (float) posY, 50.0F);
        RenderSystem.translated(posX, posY + (ent.getHeight() * scale), zOffset);

        RenderSystem.scalef((float) (-scale), (float) scale, (float) scale);
        RenderSystem.rotatef(180.0F, 0.0F, 0.0F, 1.0F);
        float f = ent.renderYawOffset;
        float f1 = ent.rotationYaw;
        float f2 = ent.rotationPitch;
        float f3 = ent.prevRotationYawHead;
        float f4 = ent.rotationYawHead;
        RenderSystem.rotatef(135.0F + rotation, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        RenderSystem.rotatef(-135.0F, 0.0F, 1.0F, 0.0F);
        RenderSystem.rotatef(-((float) Math.atan(mouseY / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
        ent.renderYawOffset = (float) Math.atan(mouseX / 40.0F) * 20.0F;
        ent.rotationYaw = (float) Math.atan(mouseX / 40.0F) * 40.0F;
        ent.rotationPitch = -((float) Math.atan(mouseY / 40.0F)) * 20.0F;
        ent.rotationYawHead = ent.rotationYaw;
        ent.prevRotationYawHead = ent.rotationYaw;
        RenderSystem.translatef(0.0F, 0.0F, 0.0F);
        EntityRendererManager rendermanager = Minecraft.getInstance().getRenderManager();
//        rendermanager.setPlayerViewY(180.0F + rotation + (drawName ? 0 : 180));
        rendermanager.setRenderShadow(false);
//        rendermanager.renderEntity(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
        rendermanager.setRenderShadow(true);
        ent.renderYawOffset = f;
        ent.rotationYaw = f1;
        ent.rotationPitch = f2;
        ent.prevRotationYawHead = f3;
        ent.rotationYawHead = f4;
        RenderSystem.popMatrix();
        RenderHelper.disableStandardItemLighting();
        RenderSystem.disableRescaleNormal();
//        RenderSystem.activeTexture(GLX.GL_TEXTURE1);
//        RenderSystem.disableTexture();
//        RenderSystem.activeTexture(GLX.GL_TEXTURE0);
    }

    public static PlayerEntity createRenderPlayer(ClientWorld world, String username) {
        return new RemoteClientPlayerEntity(world, SkullTileEntity.updateGameProfile(new GameProfile(null, username))) {
            @Override
            public String getSkinType() {
                return super.getSkinType();
            }

            @Override
            public ResourceLocation getLocationSkin() {
                ResourceLocation resourcelocation;

                Minecraft minecraft = Minecraft.getInstance();
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager().loadSkinFromCache(getGameProfile());

                if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                    resourcelocation = minecraft.getSkinManager().loadSkin(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
                } else {
                    UUID uuid = PlayerEntity.getUUID(getGameProfile());
                    resourcelocation = DefaultPlayerSkin.getDefaultSkin(uuid);
                }

                return resourcelocation;
            }

            @Override
            public boolean isWearing(PlayerModelPart part) {
                return true;
            }
        };
    }

    //    boolean animateBroken = false;
    @Override
    public boolean onUpdate() {
//        if (animate && entity != null && !invalidEntity && !animateBroken && entity.ticksExisted != BCClientEventHandler.elapsedTicks) {
//            try {
//                entity.onUpdate();
//                entity.ticksExisted = BCClientEventHandler.elapsedTicks;
//            }
//            catch (Throwable e) {
//                animateBroken = true;
//            }
//        }
        return super.onUpdate();
    }
}

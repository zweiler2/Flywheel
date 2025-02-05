package com.jozufozu.flywheel.backend.instancing;

import java.util.List;

import com.jozufozu.flywheel.Flywheel;
import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.config.FlwCommands;
import com.jozufozu.flywheel.config.FlwConfig;
import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.jozufozu.flywheel.event.ReloadRenderersEvent;
import com.jozufozu.flywheel.event.RenderLayerEvent;
import com.jozufozu.flywheel.util.AnimationTickHolder;
import com.jozufozu.flywheel.util.WorldAttached;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class InstancedRenderDispatcher {

	private static final WorldAttached<InstanceWorld> instanceWorlds = new WorldAttached<>(InstanceWorld::create);

	/**
	 * Call this when you want to manually run {@link AbstractInstance#update()}.
	 * @param blockEntity The block entity whose instance you want to update.
	 */
	public static void enqueueUpdate(BlockEntity blockEntity) {
		if (Backend.isOn() && blockEntity.hasLevel() && blockEntity.getLevel() instanceof ClientLevel) {
			instanceWorlds.get(blockEntity.getLevel())
					.getBlockEntityInstanceManager()
					.queueUpdate(blockEntity);
		}
	}

	/**
	 * Call this when you want to manually run {@link AbstractInstance#update()}.
	 * @param entity The entity whose instance you want to update.
	 */
	public static void enqueueUpdate(Entity entity) {
		if (Backend.isOn()) {
			instanceWorlds.get(entity.level())
					.getEntityInstanceManager()
					.queueUpdate(entity);
		}
	}

	public static InstanceManager<BlockEntity> getBlockEntities(LevelAccessor world) {
		return getInstanceWorld(world).getBlockEntityInstanceManager();
	}

	public static InstanceManager<Entity> getEntities(LevelAccessor world) {
		return getInstanceWorld(world).getEntityInstanceManager();
	}

	/**
	 * Get or create the {@link InstanceWorld} for the given world.
	 * @throws NullPointerException if the backend is off
	 */
	public static InstanceWorld getInstanceWorld(LevelAccessor world) {
		if (Backend.isOn()) {
			return instanceWorlds.get(world);
		} else {
			throw new NullPointerException("Backend is off, cannot retrieve instance world.");
		}
	}

	public static void tick(Minecraft mc) {
		if (!Backend.isGameActive()) {
			return;
		}
		ClientLevel world = mc.level;
		AnimationTickHolder.tick();

		if (Backend.isOn()) {
			instanceWorlds.get(world)
					.tick();
		}
	}

	public static void onBeginFrame(BeginFrameEvent event) {
		if (Backend.isGameActive() && Backend.isOn()) {
			instanceWorlds.get(event.getWorld())
					.beginFrame(event);
		}
	}

	public static void renderLayer(RenderLayerEvent event) {
		if (event.layer == null) return;

		ClientLevel world = event.getWorld();
		if (!Backend.canUseInstancing(world)) return;

		instanceWorlds.get(world).renderLayer(event);
	}

	public static void onReloadRenderers(ReloadRenderersEvent event) {
		ClientLevel world = event.getWorld();
		if (Backend.isOn() && world != null) {
			resetInstanceWorld(world);
		}
	}

	public static void resetInstanceWorld(ClientLevel world) {
		instanceWorlds.replace(world, InstanceWorld::delete)
				.loadEntities(world);
	}

	public static void getDebugString(List<String> debug) {
		debug.add("");
		debug.add("Flywheel: " + Flywheel.getVersion());

		if (Backend.isOn()) {
			InstanceWorld instanceWorld = instanceWorlds.get(Minecraft.getInstance().level);

			debug.add("Update limiting: " + FlwCommands.boolToText(FlwConfig.get().limitUpdates()).getString());
			debug.add("B: " + instanceWorld.blockEntityInstanceManager.getObjectCount() + ", E: " + instanceWorld.entityInstanceManager.getObjectCount());
			instanceWorld.engine.addDebugInfo(debug);
		} else {
			debug.add("Disabled");
		}
	}
}

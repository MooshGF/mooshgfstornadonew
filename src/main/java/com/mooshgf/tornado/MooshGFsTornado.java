package com.mooshgf.tornado;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.EnumArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Mod(MooshGFsTornado.MODID)
public class MooshGFsTornado {
    public static final String MODID = "mooshgfstornado";
    private static final List<Tornado> ACTIVE = new ArrayList<>();
    public MooshGFsTornado() { MinecraftForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mooshgfstornado")
            .requires(s -> s.hasPermission(2))
            .executes(c -> { c.getSource().sendSuccess(() -> Component.literal("MooshGF's Tornado: /mooshgfstornado help"), false); return 1; })
            .then(Commands.literal("help").executes(c -> {
                c.getSource().sendSuccess(() -> Component.literal("Commands: start <normal|supercell> [capacity 1-4], stop, capacity <1-4>, dominator <1-4>"), false); return 1;
            }))
            .then(Commands.literal("start")
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((c,b) -> { b.suggest("normal"); b.suggest("supercell"); return b.buildFuture(); })
                    .executes(c -> start(c.getSource().getLevel(), c.getSource().getPosition(), c.getSource().getEntity(), StringArgumentType.getString(c,"type"), 2))
                    .then(Commands.argument("capacity", IntegerArgumentType.integer(1,4))
                        .executes(c -> start(c.getSource().getLevel(), c.getSource().getPosition(), c.getSource().getEntity(), StringArgumentType.getString(c,"type"), IntegerArgumentType.getInteger(c,"capacity"))))))
            .then(Commands.literal("capacity").then(Commands.argument("level", IntegerArgumentType.integer(1,4)).executes(c -> {
                int cap = IntegerArgumentType.getInteger(c,"level");
                if (ACTIVE.isEmpty()) { c.getSource().sendFailure(Component.literal("No active tornado. Start one first.")); return 0; }
                ACTIVE.get(ACTIVE.size()-1).capacity = cap;
                c.getSource().sendSuccess(() -> Component.literal("Tornado capacity set to EF" + cap + " style."), true); return 1;
            })))
            .then(Commands.literal("stop").executes(c -> { int n=ACTIVE.size(); ACTIVE.clear(); c.getSource().sendSuccess(() -> Component.literal("Stopped " + n + " tornado(s)."), true); return 1; }))
            .then(Commands.literal("dominator").then(Commands.argument("number", IntegerArgumentType.integer(1,4)).executes(c -> {
                if (!(c.getSource().getEntity() instanceof ServerPlayer p)) { c.getSource().sendFailure(Component.literal("Run this in-game.")); return 0; }
                buildDominator(p.serverLevel(), p.blockPosition().offset(3,0,0), IntegerArgumentType.getInteger(c,"number"));
                c.getSource().sendSuccess(() -> Component.literal("Dominator " + IntegerArgumentType.getInteger(c,"number") + " deployed as a block-built display."), true); return 1;
            }))));
    }

    private static int start(Level level, Vec3 pos, Entity source, String type, int capacity) {
        if (!(level instanceof ServerLevel server)) return 0;
        boolean supercell = type.equalsIgnoreCase("supercell");
        if (!supercell && !type.equalsIgnoreCase("normal")) { if (source != null) source.sendSystemMessage(Component.literal("Type must be normal or supercell.")); return 0; }
        ACTIVE.add(new Tornado(server, pos.x, pos.y, pos.z, capacity, supercell));
        if (source != null) source.sendSystemMessage(Component.literal((supercell ? "Supercell" : "Tornado") + " spawned. Capacity EF" + capacity + "."));
        return 1;
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        Iterator<Tornado> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Tornado t = it.next();
            if (t.level != level) continue;
            if (++t.age > 20 * 60 * 5) { it.remove(); continue; }
            t.x += Math.sin(t.age * 0.008) * (t.supercell ? 0.035 : 0.012);
            double radius = 2.0 + t.capacity * 1.6;
            int rings = 5 + t.capacity * 2;
            for (int i=0;i<rings;i++) {
                double y = (i * 1.9 + (t.age % 20) * 0.09) % (10 + t.capacity * 3);
                double r = radius * (0.22 + 0.78 * y / (10 + t.capacity * 3));
                for (int j=0;j<5;j++) {
                    double a = t.age * 0.16 + i * 0.77 + j * Math.PI * 2 / 5;
                    double px=t.x+Math.cos(a)*r, pz=t.z+Math.sin(a)*r;
                    // Vanilla CLOUD and gust particles; deliberately no smoke particles.
                    level.sendParticles(ParticleTypes.CLOUD, px, t.y+y, pz, 1, 0.12, 0.18, 0.12, 0.015);
                    if (t.supercell && t.age % 3 == 0) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, t.y+y, pz, 1, 0.08, 0.12, 0.08, 0.01);
                }
            }
            if (t.age % 4 == 0) {
                AABB area = new AABB(t.x-radius, t.y, t.z-radius, t.x+radius, t.y+12+t.capacity*2, t.z+radius);
                for (Entity e : level.getEntities(null, area, e -> e.isAlive() && !(e instanceof ServerPlayer && e.isSpectator()))) {
                    double dx=t.x-e.getX(), dz=t.z-e.getZ(); double dist=Math.max(1.0, Math.sqrt(dx*dx+dz*dz));
                    double pull=Math.min(0.12 + t.capacity*0.035, 0.5) / dist;
                    e.push(dx*pull, 0.025 + t.capacity*0.008, dz*pull);
                    e.hurtMarked = true;
                }
            }
        }
    }

    private static void buildDominator(ServerLevel level, BlockPos origin, int n) {
        // Simple recognizable, removable-by-hand block-built Dominator prop. No custom textures/assets.
        BlockState body = switch(n) { case 1 -> Blocks.YELLOW_CONCRETE.defaultBlockState(); case 2 -> Blocks.ORANGE_CONCRETE.defaultBlockState(); case 3 -> Blocks.RED_CONCRETE.defaultBlockState(); default -> Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(); };
        for (int x=0;x<5;x++) for (int z=0;z<3;z++) level.setBlock(origin.offset(x,0,z), body, 3);
        for (int x=1;x<4;x++) for (int z=0;z<3;z++) level.setBlock(origin.offset(x,1,z), x==2 ? Blocks.GLASS.defaultBlockState() : body, 3);
        level.setBlock(origin.offset(0,1,1), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        level.setBlock(origin.offset(4,1,1), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        level.setBlock(origin.offset(1,0,-1), Blocks.BLACK_CONCRETE.defaultBlockState(), 3);
        level.setBlock(origin.offset(3,0,-1), Blocks.BLACK_CONCRETE.defaultBlockState(), 3);
        level.setBlock(origin.offset(1,0,3), Blocks.BLACK_CONCRETE.defaultBlockState(), 3);
        level.setBlock(origin.offset(3,0,3), Blocks.BLACK_CONCRETE.defaultBlockState(), 3);
    }

    private static class Tornado {
        final ServerLevel level; double x,y,z; final boolean supercell; int capacity, age=0;
        Tornado(ServerLevel l,double x,double y,double z,int c,boolean s){level=l;this.x=x;this.y=y;this.z=z;capacity=c;supercell=s;}
    }
}

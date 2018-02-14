/**
 * MobPlugin.java
 * <p>
 * Created on 17:46:07
 */
package net.twoptwoe.mobplugin;

import cn.nukkit.IPlayer;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityItem;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.event.level.ChunkPopulateEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMouseOverEntityEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.BlockFace;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.DyeColor;
import net.twoptwoe.mobplugin.entities.BaseEntity;
import net.twoptwoe.mobplugin.entities.animal.flying.Bat;
import net.twoptwoe.mobplugin.entities.animal.flying.Parrot;
import net.twoptwoe.mobplugin.entities.animal.jumping.Rabbit;
import net.twoptwoe.mobplugin.entities.animal.swimming.Squid;
import net.twoptwoe.mobplugin.entities.animal.walking.*;
import net.twoptwoe.mobplugin.entities.block.BlockEntitySpawner;
import net.twoptwoe.mobplugin.entities.monster.flying.Blaze;
import net.twoptwoe.mobplugin.entities.monster.flying.EnderDragon;
import net.twoptwoe.mobplugin.entities.monster.flying.Ghast;
import net.twoptwoe.mobplugin.entities.monster.flying.Wither;
import net.twoptwoe.mobplugin.entities.monster.jumping.MagmaCube;
import net.twoptwoe.mobplugin.entities.monster.jumping.Slime;
import net.twoptwoe.mobplugin.entities.monster.swimming.ElderGuardian;
import net.twoptwoe.mobplugin.entities.monster.swimming.Guardian;
import net.twoptwoe.mobplugin.entities.monster.walking.*;
import net.twoptwoe.mobplugin.entities.projectile.EntityFireBall;
import net.twoptwoe.mobplugin.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kniffman@googlemail.com">Michael Gertz (kniffo80)</a>
 */
public class MobPlugin extends PluginBase implements Listener {

    public static boolean MOB_AI_ENABLED = true;
    //
    // @EventHandler
    // public void PlayerMouseRightEntityEvent(PlayerMouseRightEntityEvent ev) {
    // }
    private static Class<? extends Entity>[]
            animals = new Class[]{
            Rabbit.class,
            Chicken.class,
            Cow.class,
            Horse.class,
            Pig.class,
            Sheep.class
    },
            monsters_ow = new Class[]{
                    Creeper.class,
                    Enderman.class,
                    Skeleton.class,
                    Skeleton.class,
                    Zombie.class,
                    Zombie.class,
                    ZombieVillager.class
            },
            monsters_nether = new Class[]{
                    Blaze.class,
                    Ghast.class,
                    PigZombie.class
            },
            monsters_end = new Class[]{
                    Enderman.class
            };
    private int counter = 0;

    /**
     * @param type
     * @param source
     * @param args
     * @return
     */
    public static Entity create(Object type, Position source, Object... args) {
        FullChunk chunk = source.getLevel().getChunk((int) source.x >> 4, (int) source.z >> 4, true);
        if (!chunk.isGenerated()) {
            chunk.setGenerated();
        }

        CompoundTag nbt = new CompoundTag().putList(new ListTag<DoubleTag>("Pos").add(new DoubleTag("", source.x)).add(new DoubleTag("", source.y)).add(new DoubleTag("", source.z)))
                .putList(new ListTag<DoubleTag>("Motion").add(new DoubleTag("", 0)).add(new DoubleTag("", 0)).add(new DoubleTag("", 0)))
                .putList(new ListTag<FloatTag>("Rotation").add(new FloatTag("", source instanceof Location ? (float) ((Location) source).yaw : 0))
                        .add(new FloatTag("", source instanceof Location ? (float) ((Location) source).pitch : 0)));

        return Entity.createEntity(type.toString(), chunk, nbt, args);
    }

    @Override
    public void onLoad() {
        registerEntities();
    }

    @Override
    public void onEnable() {
        // register as listener to plugin events
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getScheduler().scheduleRepeatingTask(this, new AutoSpawnTask(), 300, true);

        Utils.logServerInfo(String.format("Plugin enabled successful [aiEnabled:%s] [autoSpawnTick:%d]", MOB_AI_ENABLED, 300));
    }

    @Override
    public void onDisable() {
        Utils.logServerInfo("Plugin disabled successful.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().toLowerCase().equals("mob")) {

            if (args.length == 0) {
                sender.sendMessage("-- MobPlugin 1.0 --");
                sender.sendMessage("/mob spawn <mob> <opt:player> - Spawn a mob");
                sender.sendMessage("/mob removeall - Remove all living mobs");
                sender.sendMessage("/mob removeitems - Remove all items from ground");
            } else {
                switch (args[0]) {

                    case "spawn":
                        String mob = args[1];
                        Player playerThatSpawns = null;

                        if (args.length == 3) {
                            playerThatSpawns = this.getServer().getPlayer(args[2]);
                        } else {
                            playerThatSpawns = (Player) sender;
                        }

                        if (playerThatSpawns != null) {
                            Position pos = playerThatSpawns.getPosition();

                            Entity ent;
                            if ((ent = MobPlugin.create(mob, pos)) != null) {
                                ent.spawnToAll();
                                sender.sendMessage("Spawned " + mob + " to " + playerThatSpawns.getName());
                            } else {
                                sender.sendMessage("Unable to spawn " + mob);
                            }
                        } else {
                            sender.sendMessage("Unknown player " + (args.length == 3 ? args[2] : ((Player) sender).getName()));
                        }
                        break;
                    case "removeall":
                        int count = 0;
                        for (Level level : getServer().getLevels().values()) {
                            for (Entity entity : level.getEntities()) {
                                if (entity instanceof BaseEntity) {
                                    entity.close();
                                    count++;
                                }
                            }
                        }
                        sender.sendMessage("Removed " + count + " entities from all levels.");
                        break;
                    case "removeitems":
                        count = 0;
                        for (Level level : getServer().getLevels().values()) {
                            for (Entity entity : level.getEntities()) {
                                if (entity instanceof EntityItem && entity.isOnGround()) {
                                    entity.close();
                                    count++;
                                }
                            }
                        }
                        sender.sendMessage("Removed " + count + " items on ground from all levels.");
                        break;
                    default:
                        sender.sendMessage("Unkown command.");
                        break;
                }
            }
        }
        return true;
    }

    private void registerEntities() {
        // register living entities
        Entity.registerEntity(Bat.class.getSimpleName(), Bat.class);
        Entity.registerEntity(Chicken.class.getSimpleName(), Chicken.class);
        Entity.registerEntity(Cow.class.getSimpleName(), Cow.class);
        Entity.registerEntity(Donkey.class.getSimpleName(), Donkey.class);
        Entity.registerEntity(Horse.class.getSimpleName(), Horse.class);
        Entity.registerEntity(MagmaCube.class.getSimpleName(), MagmaCube.class);
        Entity.registerEntity(Llama.class.getSimpleName(), Llama.class);
        Entity.registerEntity(Mooshroom.class.getSimpleName(), Mooshroom.class);
        Entity.registerEntity(Mule.class.getSimpleName(), Mule.class);
        Entity.registerEntity(Ocelot.class.getSimpleName(), Ocelot.class);
        Entity.registerEntity(Parrot.class.getSimpleName(), Parrot.class);
        Entity.registerEntity(Pig.class.getSimpleName(), Pig.class);
        Entity.registerEntity(PolarBear.class.getSimpleName(), PolarBear.class);
        Entity.registerEntity(Rabbit.class.getSimpleName(), Rabbit.class);
        Entity.registerEntity(Sheep.class.getSimpleName(), Sheep.class);
        Entity.registerEntity(SkeletonHorse.class.getSimpleName(), SkeletonHorse.class);
        Entity.registerEntity(Squid.class.getSimpleName(), Squid.class);
        Entity.registerEntity(Villager.class.getSimpleName(), Villager.class);
        Entity.registerEntity(ZombieHorse.class.getSimpleName(), ZombieHorse.class);

        Entity.registerEntity(Blaze.class.getSimpleName(), Blaze.class);
        Entity.registerEntity(Ghast.class.getSimpleName(), Ghast.class);
        Entity.registerEntity(CaveSpider.class.getSimpleName(), CaveSpider.class);
        Entity.registerEntity(Creeper.class.getSimpleName(), Creeper.class);
        Entity.registerEntity(ElderGuardian.class.getSimpleName(), ElderGuardian.class);
        Entity.registerEntity(EnderDragon.class.getSimpleName(), EnderDragon.class);
        Entity.registerEntity(Enderman.class.getSimpleName(), Enderman.class);
        Entity.registerEntity(Endermite.class.getSimpleName(), Endermite.class);
        Entity.registerEntity(Guardian.class.getSimpleName(), Guardian.class);
        Entity.registerEntity(Husk.class.getSimpleName(), Husk.class);
        Entity.registerEntity(IronGolem.class.getSimpleName(), IronGolem.class);
        Entity.registerEntity(PigZombie.class.getSimpleName(), PigZombie.class);
        Entity.registerEntity(Shulker.class.getSimpleName(), Shulker.class);
        Entity.registerEntity(Silverfish.class.getSimpleName(), Silverfish.class);
        Entity.registerEntity(Skeleton.class.getSimpleName(), Skeleton.class);
        Entity.registerEntity(Slime.class.getSimpleName(), Slime.class);
        Entity.registerEntity(SnowGolem.class.getSimpleName(), SnowGolem.class);
        Entity.registerEntity(Spider.class.getSimpleName(), Spider.class);
        Entity.registerEntity(Stray.class.getSimpleName(), Stray.class);
        Entity.registerEntity(Witch.class.getSimpleName(), Witch.class);
        Entity.registerEntity(Wither.class.getSimpleName(), Wither.class);
        Entity.registerEntity(WitherSkeleton.class.getSimpleName(), WitherSkeleton.class);
        Entity.registerEntity(Wolf.class.getSimpleName(), Wolf.class);
        Entity.registerEntity(Zombie.class.getSimpleName(), Zombie.class);
        Entity.registerEntity(ZombieVillager.class.getSimpleName(), ZombieVillager.class);

        // register the fireball entity
        Entity.registerEntity("FireBall", EntityFireBall.class);

        // register the mob spawner (which is probably not needed anymore)
        BlockEntity.registerBlockEntity("MobSpawner", BlockEntitySpawner.class);
    }

    /**
     * Returns all registered players to the current server
     *
     * @return a {@link List} containing a number of {@link IPlayer} elements,
     * which can be {@link Player}
     */
    public List<IPlayer> getAllRegisteredPlayers() {
        List<IPlayer> playerList = new ArrayList<>();
        for (Player player : this.getServer().getOnlinePlayers().values()) {
            playerList.add(player);
        }
        return playerList;
    }

    // --- event listeners ---

    /**
     * checks if a given player name's player instance is already in the given
     * list
     *
     * @param name       the name of the player to be checked
     * @param playerList the existing entries
     * @return <code>true</code> if the player is already in the list
     */
    private boolean isPlayerAlreadyInList(String name, List<IPlayer> playerList) {
        for (IPlayer player : playerList) {
            if (player.getName().toLowerCase().equals(name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * This event is called when an entity dies. We need this for experience
     * gain.
     *
     * @param ev the event that is received
     */
    @EventHandler
    public void EntityDeathEvent(EntityDeathEvent ev) {
        if (ev.getEntity() instanceof BaseEntity) {
            BaseEntity baseEntity = (BaseEntity) ev.getEntity();
            if (baseEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                Entity damager = ((EntityDamageByEntityEvent) baseEntity.getLastDamageCause()).getDamager();
                if (damager instanceof Player) {
                    Player player = (Player) damager;
                    int killExperience = baseEntity.getKillExperience();
                    if (killExperience > 0 && player != null && player.isSurvival()) {
                        player.addExperience(killExperience);
                        // don't drop that fucking experience orbs because they're somehow buggy :(
                        // if (player.isSurvival()) {
                        // for (int i = 1; i <= killExperience; i++) {
                        // player.getLevel().dropExpOrb(baseEntity, 1);
                        // }
                        // }
                    }
                }
            }
        }
    }

    @EventHandler
    public void PlayerInteractEvent(PlayerInteractEvent ev) {
        if (ev.getFace() == null || ev.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Item item = ev.getItem();
        Block block = ev.getBlock();
        if (item.getId() == Item.SPAWN_EGG && block.getId() == Item.MONSTER_SPAWNER) {
            ev.setCancelled(true);

            BlockEntity blockEntity = block.getLevel().getBlockEntity(block);
            if (blockEntity != null && blockEntity instanceof BlockEntitySpawner) {
                ((BlockEntitySpawner) blockEntity).setSpawnEntityType(item.getDamage());
            } else {
                if (blockEntity != null) {
                    blockEntity.close();
                }
                CompoundTag nbt = new CompoundTag().putString("id", BlockEntity.MOB_SPAWNER).putInt("EntityId", item.getDamage()).putInt("x", (int) block.x).putInt("y", (int) block.y).putInt("z",
                        (int) block.z);

                new BlockEntitySpawner(block.getLevel().getChunk((int) block.x >> 4, (int) block.z >> 4), nbt);
            }
        }
    }

    @EventHandler
    public void BlockPlaceEvent(BlockPlaceEvent ev) {
        if (ev.isCancelled()) {
            return;
        }

        Block block = ev.getBlock();
        if (block.getId() == Item.JACK_O_LANTERN || block.getId() == Item.PUMPKIN) {
            if (block.getSide(BlockFace.DOWN).getId() == Item.SNOW_BLOCK && block.getSide(BlockFace.DOWN, 2).getId() == Item.SNOW_BLOCK) {
                Entity entity = create("SnowGolem", block.add(0.5, -2, 0.5));
                if (entity != null) {
                    entity.spawnToAll();
                }

                ev.setCancelled();
                block.getLevel().setBlock(block.add(0, -1, 0), new BlockAir());
                block.getLevel().setBlock(block.add(0, -2, 0), new BlockAir());
            } else if (block.getSide(BlockFace.DOWN).getId() == Item.IRON_BLOCK && block.getSide(BlockFace.DOWN, 2).getId() == Item.IRON_BLOCK) {
                block = block.getSide(BlockFace.DOWN);

                Block first, second = null;
                if ((first = block.getSide(BlockFace.EAST)).getId() == Item.IRON_BLOCK && (second = block.getSide(BlockFace.WEST)).getId() == Item.IRON_BLOCK) {
                    block.getLevel().setBlock(first, new BlockAir());
                    block.getLevel().setBlock(second, new BlockAir());
                } else if ((first = block.getSide(BlockFace.NORTH)).getId() == Item.IRON_BLOCK && (second = block.getSide(BlockFace.SOUTH)).getId() == Item.IRON_BLOCK) {
                    block.getLevel().setBlock(first, new BlockAir());
                    block.getLevel().setBlock(second, new BlockAir());
                }

                if (second != null) {
                    Entity entity = MobPlugin.create("IronGolem", block.add(0.5, -1, 0.5));
                    if (entity != null) {
                        entity.spawnToAll();
                    }
                    block.getLevel().setBlock(block, new BlockAir());
                    block.getLevel().setBlock(block.add(0, -1, 0), new BlockAir());
                    ev.setCancelled();
                }
            }
        }
    }

    @EventHandler
    public void BlockBreakEvent(BlockBreakEvent ev) {
        if (ev.isCancelled()) {
            return;
        }

        Block block = ev.getBlock();
        if ((block.getId() == Block.MONSTER_EGG)
                && block.getLevel().getBlockLightAt((int) block.x, (int) block.y, (int) block.z) < 12 && Utils.rand(1, 5) == 1) {

            Silverfish entity = (Silverfish) create("Silverfish", block.add(0.5, 0, 0.5));
            if (entity != null) {
                entity.spawnToAll();
            }
        }
    }

    @EventHandler
    public void PlayerMouseOverEntityEvent(PlayerMouseOverEntityEvent ev) {
        if (this.counter > 10) {
            counter = 0;
            // wolves can be tamed using bones
            if (ev != null && ev.getEntity() != null && ev.getPlayer() != null && ev.getEntity().getNetworkId() == Wolf.NETWORK_ID && ev.getPlayer().getInventory().getItemInHand().getId() == Item.BONE) {
                // check if already owned and tamed ...
                Wolf wolf = (Wolf) ev.getEntity();
                if (!wolf.isAngry() && wolf.getOwner() == null) {
                    // now try it out ...
                    EntityEventPacket packet = new EntityEventPacket();
                    packet.eid = ev.getEntity().getId();
                    packet.event = EntityEventPacket.TAME_SUCCESS;
                    Server.broadcastPacket(new Player[]{ev.getPlayer()}, packet);

                    // set the owner
                    wolf.setOwner(ev.getPlayer());
                    wolf.setCollarColor(DyeColor.BLUE);
                    wolf.saveNBT();
                }
            }
        } else {
            counter++;
        }
    }

    public static void ChunkPopulateEvent(ChunkPopulateEvent event) {
        if (EnumDimension.getFromWorld(event.getLevel()) == EnumDimension.OVERWORLD && Utils.rand(0, 12) == 2) {
            //spawn random pack of animals
            Class<? extends Entity> entity = animals[Utils.rand(0, animals.length)];
            FullChunk chunk = event.getChunk();
            int count = Utils.rand(3, 6);
            for (int i = 0; i < count; i++) {
                int xPos = (chunk.getX() << 4) | Utils.rand(0, 15);
                int zPos = (chunk.getZ() << 4) | Utils.rand(0, 15);
                int yPos = chunk.getHighestBlockAt(xPos & 0xF, zPos & 0xF);
                if (yPos <= 64) {
                    return;
                }
                Entity entityObj = create(entity.getSimpleName(), new Position(xPos, yPos, zPos, event.getLevel()));
                event.getLevel().addEntity(entityObj);
            }
        }
    }

    public static void spawnMobs()  {
        Server server = Server.getInstance();
        for (Player player : server.getOnlinePlayers().values()) {
            Class<? extends Entity>[] arr = null;
            Level level = player.getLevel();
            switch (EnumDimension.getFromWorld(level))  {
                case OVERWORLD:
                    arr = monsters_ow;
                    break;
                case NETHER:
                    arr = monsters_nether;
                    break;
                case THE_END:
                    arr = monsters_end;
            }
            Class<? extends Entity> clazz = arr[Utils.rand(0, arr.length)];
            int count = Math.max(1, Utils.rand(-3, 2));
            int xBase = Utils.rand(-32, 32) + player.getFloorX();
            int zBase = Utils.rand(-32, 32) + player.getFloorZ();
            for (int i = 0; i < count; i++) {
                int xPos = Utils.rand(-5, 5) + xBase;
                int zPos = Utils.rand(-5, 5) + zBase;
                int yPos = level.getHighestBlockAt(xPos, zPos);
                Entity entity = create(clazz.getSimpleName(), new Location(xPos, yPos, zPos, level));
                level.addEntity(entity);
            }
        }
    }
}

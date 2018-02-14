/**
 * EntitySpawner.java
 * <p>
 * Created on 10:38:53
 */
package de.kniffo80.mobplugin.entities.autospawn;

import cn.nukkit.IPlayer;
import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import de.kniffo80.mobplugin.EnumDimension;

import java.util.List;

/**
 * @author <a href="mailto:kniffman@googlemail.com">Michael Gertz</a>
 */
public interface IEntitySpawner {

    void spawn(List<Player> onlinePlayers);

    SpawnResult spawn(IPlayer iPlayer, Position pos, Level level);

    int getEntityNetworkId();

    String getEntityName();

    boolean canSpawnIn(EnumDimension dimension);
}

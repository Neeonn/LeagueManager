package io.github.divinerealms.leaguemanager.gui.impl;

import io.github.divinerealms.leaguemanager.configs.Config;
import io.github.divinerealms.leaguemanager.gui.InventoryButton;
import io.github.divinerealms.leaguemanager.gui.InventoryGUI;
import io.github.divinerealms.leaguemanager.managers.DataManager;
import io.github.divinerealms.leaguemanager.managers.GUIManager;
import io.github.divinerealms.leaguemanager.managers.UtilManager;
import io.github.divinerealms.leaguemanager.utils.Helper;
import io.github.divinerealms.leaguemanager.utils.Logger;
import lombok.Getter;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

@Getter
public class RostersGUI extends InventoryGUI {
  private final UtilManager utilManager;
  private final Logger logger;
  private final Helper helper;
  private final GUIManager guiManager;
  private final DataManager dataManager;
  private static final YamlConfiguration config = Config.getConfig("config.yml");
  private final String teamData = "teamdata";

  public RostersGUI(final UtilManager utilManager, final GUIManager guiManager) {
    this.utilManager = utilManager;
    this.logger = utilManager.getLogger();
    this.helper = utilManager.getHelper();
    this.guiManager = guiManager;
    this.dataManager = new DataManager(utilManager.getPlugin());
  }

  @Override
  public Inventory createInventory() {
    return Bukkit.createInventory(null, 6 * 9, "Rosteri");
  }
  
  @Override
  public void decorate(Player player) {
    for (int slot = 0; slot <= 53; slot++) {
      if (slot == 37) {
        this.addButton(slot, this.createHead("&fPomoćnik", "18154", getUtilManager().color("&eLevi klik &7za pregled rostera."), getUtilManager().color("&eDesni klik &7za tp do stadiona."))
            .consumer(event -> {
              Player target = (Player) event.getWhoClicked();
              target.closeInventory();
              target.performCommand("rosters help");
            }));
      } else if (slot == 43) {
        this.addButton(slot, this.createHead("&cZatvorite", "3229")
            .consumer(event -> event.getWhoClicked().closeInventory()));
      } else this.addButton(slot, this.createButton("&r", (byte) 7));
    }

    processTeamConfig("main", 10);
    processTeamConfig("juniors", 19);

    super.decorate(player);
  }

  private void processTeamConfig(String configType, int slot) {
    getDataManager().setConfig(getTeamData(), configType);

    for (String teamName : getDataManager().getConfig(configType).getKeys(false)) {
      if (!getHelper().groupExists(teamName)) return;
      else if ((configType.equals("main") && getHelper().groupHasMeta(teamName, "team")) ||
          (configType.equals("juniors") && getHelper().groupHasMeta(teamName, "b"))) {

        int teamSize = getDataManager().getConfig(configType).get(teamName + ".players") != null ? getDataManager().getConfig(configType).getStringList(teamName + ".players").size() : 0;
        ItemStack banner = getDataManager().getConfig(configType).get(teamName + ".banner") != null ? (ItemStack) getDataManager().getConfig(configType).get(teamName + ".banner") : new ItemStack(Material.BANNER, 1, (byte) (configType.equals("main") ? 15 : 10));

        String teamDisplayName = (configType.equals("main") ? "&f&l" : "&a&l") + getDataManager().getConfig(configType).getString(teamName + ".name", "&c/");
        String tag = getUtilManager().color("&fTag: " + getDataManager().getConfig(configType).getString(teamName + ".tag", "/"));
        String manager = getUtilManager().color("&fMenadžer: &a" + getDataManager().getConfig(configType).getString(teamName + ".manager"));
        String captain = getUtilManager().color("&fKapiten: &c" + getDataManager().getConfig(configType).getString(teamName + ".captain", "/"));
        String teamInfo = getUtilManager().color("&7&oTim ima " + teamSize + " igrača");

        if (slot == 17 || slot == 18 || slot == 27) slot = slot+2;

        this.addButton(slot <= (configType.equals("main") ? 16 : 25) ? slot++ : slot,
            this.createTeamItem(banner, teamDisplayName, teamName, "", tag,
                getDataManager().getConfig(configType).getString(teamName + ".manager") != null ? manager : "",
                getDataManager().getConfig(configType).getString(teamName + ".captain") != null ? captain : "", "", teamInfo));
      }
    }
  }

  private InventoryButton createTeamItem(ItemStack itemStack, String title, String teamName, String... lore) {
    ItemMeta itemMeta = itemStack.getItemMeta();
    itemMeta.setDisplayName(getUtilManager().color(title));
    itemMeta.setLore(Arrays.asList(lore));
    itemStack.setItemMeta(itemMeta);
    return new InventoryButton()
        .creator(player -> itemStack)
        .consumer(event -> {
          getGuiManager().setTeamName(teamName);
          Player player = (Player) event.getWhoClicked();
          player.closeInventory();
          if (event.getClick().isRightClick()) player.performCommand("warp " + teamName + "top");
          else getGuiManager().openGUI(new PerRosterGUI(getUtilManager(), getGuiManager()), player);
        });
  }

  private InventoryButton createButton(String title, byte damage, String... lore) {
    ItemStack button = new ItemStack(Material.STAINED_GLASS_PANE, 1, damage);
    ItemMeta buttonMeta = button.getItemMeta();
    buttonMeta.setDisplayName(getUtilManager().color(title));
    buttonMeta.setLore(Arrays.asList(lore));
    button.setItemMeta(buttonMeta);
    return new InventoryButton()
        .creator(player -> button)
        .consumer(event -> {});
  }

  private InventoryButton createHead(String title, String headId, String... lore) {
    HeadDatabaseAPI headDatabaseAPI = new HeadDatabaseAPI();
    ItemStack head = null;
    try {
      head = headDatabaseAPI.getItemHead(headId);
      ItemMeta headMeta = head.getItemMeta();
      headMeta.setDisplayName(getUtilManager().color(title));
      headMeta.setLore(Arrays.asList(lore));
      head.setItemMeta(headMeta);
    } catch (NullPointerException exception) {
      getLogger().send("helper", "nemoguće pronaći glavu " + headId);
    }
    ItemStack finalHead = head;
    return new InventoryButton()
        .creator(player -> finalHead != null ? finalHead : new ItemStack(Material.BARRIER))
        .consumer(event -> {});
  }
}

package net.sacredlabyrinth.Phaed.PreciousStones.entries;

import net.sacredlabyrinth.Phaed.PreciousStones.PreciousStones;
import org.bukkit.Location;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author phaed
 */
public class PlayerEntry
{
    private String name;
    private boolean disabled;
    private boolean online;
    private int density;
    private boolean superduperpickaxe;
    private Location outsideLocation;
    private Map<BlockTypeEntry, Integer> fieldCount = new HashMap<BlockTypeEntry, Integer>();
    private JSONArray confiscatedInventory = new JSONArray();
    private ItemStackEntry confiscatedHelmet = null;
    private ItemStackEntry confiscatedChestplate = null;
    private ItemStackEntry confiscatedLeggings = null;
    private ItemStackEntry confiscatedBoots = null;

    /**
     * @param disabled
     */
    public PlayerEntry()
    {
        disabled = PreciousStones.getInstance().getSettingsManager().isOffByDefault();
        density = PreciousStones.getInstance().getSettingsManager().getVisualizeDensity();
    }

    /**
     * Adds in the confiscated inventory and items
     *
     * @param items
     * @param helmet
     * @param chestplate
     * @param leggings
     * @param boots
     */
    public void confiscate(List<ItemStackEntry> items, ItemStackEntry helmet, ItemStackEntry chestplate, ItemStackEntry leggings, ItemStackEntry boots)
    {
        for (ItemStackEntry entry : items)
        {
            confiscatedInventory.add(entry.serialize());
        }

        if (helmet != null)
        {
            confiscatedHelmet = helmet;
        }

        if (chestplate != null)
        {
            confiscatedChestplate = chestplate;
        }

        if (leggings != null)
        {
            confiscatedLeggings = leggings;
        }

        if (boots != null)
        {
            confiscatedBoots = boots;
        }
    }

    /**
     * Returns the list of confiscated items, and removes them from the entry
     *
     * @return
     */
    public List<ItemStackEntry> returnInventory()
    {
        List<ItemStackEntry> out = new ArrayList<ItemStackEntry>();

        for(Object stackEntry : confiscatedInventory)
        {
            out.add(new ItemStackEntry((JSONObject) stackEntry));
        }

        confiscatedInventory.clear();
        return out;
    }

    /**
     * Returns confiscated helmet
     *
     * @return
     */
    public ItemStackEntry returnHelmet()
    {
        ItemStackEntry out = confiscatedHelmet;
        confiscatedHelmet = null;
        return out;
    }

    /**
     * Returns confiscated chestplate
     *
     * @return
     */
    public ItemStackEntry returnChestplate()
    {
        ItemStackEntry out = confiscatedChestplate;
        confiscatedChestplate = null;
        return out;
    }

    /**
     * Returns confiscated leggings
     *
     * @return
     */
    public ItemStackEntry returnLeggings()
    {
        ItemStackEntry out = confiscatedLeggings;
        confiscatedLeggings = null;
        return out;
    }

    /**
     * Returns confiscated boots
     *
     * @return
     */
    public ItemStackEntry returnBoots()
    {
        ItemStackEntry out = confiscatedBoots;
        confiscatedBoots = null;
        return out;
    }

    /**
     * Increment the field count of a specific field
     *
     * @param typeid
     */
    public void incrementFieldCount(BlockTypeEntry type)
    {
        if (fieldCount.containsKey(type))
        {
            fieldCount.put(type, fieldCount.get(type) + 1);
        }
        else
        {
            fieldCount.put(type, 1);
        }
    }

    /**
     * Decrement the field count of a specific field
     *
     * @param typeid
     */
    public void decrementFieldCount(BlockTypeEntry type)
    {
        if (fieldCount.containsKey(type))
        {
            fieldCount.put(type, Math.max(fieldCount.get(type) - 1, 0));
        }
    }

    /**
     * @return the fieldCount
     */
    public HashMap<BlockTypeEntry, Integer> getFieldCount()
    {
        HashMap<BlockTypeEntry, Integer> counts = new HashMap<BlockTypeEntry, Integer>();
        counts.putAll(fieldCount);

        return counts;
    }

    /**
     * Get the number of fields the player has placed
     *
     * @param typeid
     * @return
     */
    public int getFieldCount(BlockTypeEntry type)
    {
        if (fieldCount.containsKey(type))
        {
            return fieldCount.get(type);
        }

        return 0;
    }

    /**
     * Get the total number of fields the player has placed
     *
     * @param typeid
     * @return
     */
    public int getTotalFieldCount()
    {
        int total = 0;

        for (int count : fieldCount.values())
        {
            total += count;
        }

        return total;
    }

    /**
     * @return
     */
    public boolean isDisabled()
    {
        return this.disabled;
    }

    /**
     * @param disabled
     */
    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    /**
     * @return the online
     */
    public boolean isOnline()
    {
        return online;
    }

    /**
     * @param online the online to set
     */
    public void setOnline(boolean online)
    {
        this.online = online;
    }

    /**
     * @return the outsideLocation
     */
    public Location getOutsideLocation()
    {
        return outsideLocation;
    }

    /**
     * @param outsideLocation the outsideLocation to set
     */
    public void setOutsideLocation(Location outsideLocation)
    {
        this.outsideLocation = outsideLocation;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Return the list of flags and their data as a json string
     *
     * @return the flags
     */
    public String getFlags()
    {
        JSONObject json = new JSONObject();

        // writing the list of flags to json

        if (superduperpickaxe)
        {
            json.put("superduperpickaxe", superduperpickaxe);
        }

        if (disabled)
        {
            json.put("disabled", disabled);
        }

        if (!confiscatedInventory.isEmpty())
        {
            json.put("confiscated", confiscatedInventory);
        }

        if (confiscatedHelmet != null)
        {
            json.put("helmet", confiscatedHelmet.serialize());
        }

        if (confiscatedChestplate != null)
        {
            json.put("chestplate", confiscatedChestplate.serialize());
        }

        if (confiscatedLeggings != null)
        {
            json.put("leggings", confiscatedLeggings.serialize());
        }

        if (confiscatedBoots != null)
        {
            json.put("boots", confiscatedBoots.serialize());
        }

        json.put("density", density);

        return json.toString();
    }

    /**
     * Read the list of flags in from a json string
     *
     * @param flagString the flags to set
     */
    public void setFlags(String flagString)
    {
        if (flagString != null && !flagString.isEmpty())
        {
            Object obj = JSONValue.parse(flagString);
            JSONObject flags = (JSONObject) obj;

            if (flags != null)
            {
                for (Object flag : flags.keySet())
                {
                    try
                    {
                        // reading the list of flags from json

                        if (flag.equals("disabled"))
                        {
                            disabled = (Boolean) flags.get(flag);
                        }

                        if (flag.equals("superduperpickaxe"))
                        {
                            superduperpickaxe = (Boolean) flags.get(flag);
                        }

                        if (flag.equals("density"))
                        {
                            density = ((Long) flags.get(flag)).intValue();
                        }

                        if (flag.equals("confiscated"))
                        {
                            confiscatedInventory = ((JSONArray) flags.get(flag));
                        }

                        if (flag.equals("helmet"))
                        {
                            confiscatedHelmet = new ItemStackEntry((JSONObject)flags.get(flag));
                        }

                        if (flag.equals("chestplate"))
                        {
                            confiscatedChestplate = new ItemStackEntry((JSONObject)flags.get(flag));
                        }

                        if (flag.equals("leggings"))
                        {
                            confiscatedLeggings = new ItemStackEntry((JSONObject)flags.get(flag));
                        }

                        if (flag.equals("boots"))
                        {
                            confiscatedBoots = new ItemStackEntry((JSONObject)flags.get(flag));
                        }
                    }
                    catch (Exception ex)
                    {
                        System.out.print("Failed reading flag: " + flag);
                        System.out.print("Value: " + flags.get(flag));
                        System.out.print("Error: " + ex.getMessage());

                        for (StackTraceElement el : ex.getStackTrace())
                        {
                            System.out.print(el.toString());
                        }
                    }
                }
            }
        }
    }

    public int getDensity()
    {
        return Math.max(density, 1);
    }

    public void setDensity(int density)
    {
        this.density = density;
    }

    public boolean isSuperduperpickaxe()
    {
        return superduperpickaxe;
    }

    public void setSuperduperpickaxe(boolean superduperpickaxe)
    {
        this.superduperpickaxe = superduperpickaxe;
    }
}

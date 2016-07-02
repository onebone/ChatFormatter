package me.onebone.chatformatter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;

public class ChatFormatter extends PluginBase implements Listener{
	private Map<String, String> lang, players;
	
	public void setPrefix(String player, String prefix){
		this.players.put(player.toLowerCase(), prefix);
		Config config = new Config(new File(this.getDataFolder(), "players.yml"));
		config.setAll(new LinkedHashMap<String, Object>(this.players));
		config.save();
	}
	
	public void onEnable(){
		this.saveDefaultConfig();
		
		String name = this.getConfig().get("language", "eng");
		InputStream is = this.getResource("lang_" + name + ".json");
		if(is == null){
			this.getLogger().critical("Could not load language file. Changing to default.");
			
			is = this.getResource("lang_eng.json");
		}
		
		try{
			lang = new GsonBuilder().create().fromJson(Utils.readFile(is), new TypeToken<LinkedHashMap<String, String>>(){}.getType());
		}catch(JsonSyntaxException | IOException e){
			this.getLogger().critical(e.getMessage());
		}
		
		if(!name.equals("eng")){
			try{
				LinkedHashMap<String, String> temp = new GsonBuilder().create().fromJson(Utils.readFile(this.getResource("lang_eng.json")), new TypeToken<LinkedHashMap<String, String>>(){}.getType());
				temp.forEach((k, v) -> {
					if(!lang.containsKey(k)){
						lang.put(k, v);
					}
				});
			}catch(IOException e){
				this.getLogger().critical(e.getMessage());
			}
		}
		
		Config config = new Config(new File(this.getDataFolder(), "players.yml"));
		players = new HashMap<>();
		config.getAll().forEach((k, v) -> {
			players.put(k.toLowerCase(), v.toString());
		});
		
		this.getServer().getPluginManager().registerEvents(this, this);
	}
	
	@EventHandler
	public void onChat(PlayerChatEvent event){
		event.setFormat(this.getFormat(event.getPlayer()));
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(command.getName().equals("cformat")){
			if(args.length < 1){
				sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
				return true;
			}
			
			args[0] = args[0].toLowerCase();
			if(args[0].equals("add")){
				if(args.length < 3){
					sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
					return true;
				}
				
				String name = args[1];
				String format = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
				
				Map<String, String> formats = this.getConfig().get("format", new HashMap<String, String>());
				if(formats.containsKey(name)){
					sender.sendMessage(this.getMessage("format-exists", name));
					return true;
				}
				
				formats.put(name, format);
				sender.sendMessage(this.getMessage("format-added"));
				
				this.getConfig().set("format", formats);
				this.getConfig().save();
			}else if(args[0].equals("set")){
				if(args.length < 3){
					sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
					return true;
				}
				
				String player = args[1];
				String name = args[2];
				
				this.players.put(player.toLowerCase(), name);
				Config config = new Config(new File(this.getDataFolder(), "players.yml"));
				config.setAll(new LinkedHashMap<String, Object>(this.players));
				config.save();
				
				sender.sendMessage(this.getMessage("tag-set", player, name));
			}else if(args[0].equals("reload")){
				this.getConfig().reload();
				Config config = new Config(new File(this.getDataFolder(), "players.yml"));
				players.clear();
				config.getAll().forEach((k, v) -> {
					players.put(k.toLowerCase(), v.toString());
				});
			}else{
				sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
			}
			return true;
		}
		
		return false;
	}
	
	public String getMessage(String key){
		return this.getMessage(key);
	}
	
	public String getMessage(String key, Object... params){
		if(this.lang.containsKey(key)){
			return replaceMessage(this.lang.get(key), params);
		}
		return "Could not find message with " + key;
	}
	
	private String replaceMessage(String lang, Object... params){
		StringBuilder builder = new StringBuilder();
		
		for(int i = 0; i < lang.length(); i++){
			char c = lang.charAt(i);
			if(c == '{'){
				int index;
				if((index = lang.indexOf('}', i)) != -1){
					try{
						String p = lang.substring(i + 1, index);
						int param = Integer.parseInt(p);
						
						if(params.length > param){
							i = index;
							
							builder.append(params[param]);
							continue;
						}
					}catch(NumberFormatException e){}
				}
			}else if(c == '&'){
				char color = lang.charAt(++i);
				if((color >= '0' && color <= 'f') || color == 'r' || color == 'l' || color == 'o'){
					builder.append(TextFormat.ESCAPE);
					builder.append(color);
					continue;
				}
			}
			
			builder.append(c);
		}
		
		return builder.toString();
	}
	
	public String getFormat(Player player){
		if(this.players.containsKey(player.getName().toLowerCase())){
			String tag = this.players.get(player.getName().toLowerCase());
			
			Map<String, String> format = this.getConfig().get("format", new HashMap<String, String>());
			if(format.containsKey(tag)){
				return format.get(tag).replace("{%tag}", tag);
			}else{
				return this.getConfig().getString("default.format", "<[{%tag}] {%0}> {%1}").replace("{%tag}", tag);
			}
		}
		
		return this.getConfig().getString("default.format", "<[{%tag}] {%0}> {%1}").replace("{%tag}", this.getConfig().getString("default.tag"));
	}
}

package name.teemo.pogo;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.hisunsray.commons.res.Config;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.Inventories;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.api.player.PlayerProfile;
import com.pokegoapi.api.pokemon.Pokemon;

import name.teemo.pogo.task.CatchNearbyPokemon;
import name.teemo.pogo.task.DropUselessItems;
import name.teemo.pogo.task.GetMapRandomDirection;
import name.teemo.pogo.task.HatchEggs;
import name.teemo.pogo.task.LootNearbyPokestop;
import name.teemo.pogo.task.ReleasePokemon;
import name.teemo.pogo.task.UpdateProfile;
import name.teemo.pogo.utils.PokemonName;
import name.teemo.pogo.utils.ThreadCount;

public class PokemonGoBot implements Runnable{
	private static Logger logger = Logger.getLogger(PokemonGoBot.class);
	
	private PokemonGo pokemonGo;
	public PokemonGoBot(PokemonGo pokemonGo) {
		this.pokemonGo = pokemonGo;
	}
	@Override
	public void run() {
		
		logger.info("正在获取账号信息...");
		PlayerProfile playerProfile = null;
		Inventories inventories = null;
		
		while (playerProfile == null || inventories == null) {
			try{
				playerProfile = pokemonGo.getPlayerProfile();
				Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
				inventories = pokemonGo.getInventories();
			}catch (Exception e) {
				logger.error("系统等待出现异常...");
			}finally {
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException e) {
					logger.error("系统等待出现异常...");
					e.printStackTrace();
				}
			}
		}
		try{
			logger.info("用户名称:"	+ playerProfile.getPlayerData().getUsername());
			logger.info("战队信息:"	+ playerProfile.getPlayerData().getTeam().name());
			logger.info("口袋金币:"	+ playerProfile.getCurrency(PlayerProfile.Currency.POKECOIN));
			logger.info("星尘数量:"	+ playerProfile.getCurrency(PlayerProfile.Currency.STARDUST));
			logger.info("达成等级:"	+ playerProfile.getStats().getLevel());
			logger.info("当前经验:"	+ playerProfile.getStats().getExperience());
			logger.info("口袋银行:"	+ inventories.getPokebank().getPokemons().size() + "/" + playerProfile.getPlayerData().getMaxPokemonStorage());
			logger.info("道具包裹:"	+ inventories.getItemBag().getItems().size() + "/" + playerProfile.getPlayerData().getMaxItemStorage());
		}catch (Exception e) {
			logger.error("系统无法调用PokeAPI资源");
			e.printStackTrace();
		}
		
		List<Pokemon> pokemons = inventories.getPokebank().getPokemons();
		ComparatorPM comparatorPM = new ComparatorPM();
		Collections.sort(pokemons, comparatorPM);
		DecimalFormat decimalFormat = new DecimalFormat("#.##");   
		if(pokemons.size() > 0 ){
			logger.info("持有宝可梦:<NAME/CP/IV>");
		}
		for(Pokemon pokemon : pokemons){
			logger.info("名称:" + PokemonName.getPokemonName(pokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")) + " / CP:" + pokemon.getCp() + " / IV:" + Double.parseDouble(decimalFormat.format(pokemon.getIvRatio()))*100 + "%");
		}
		
		logger.info("正在获取补给站信息...");
		MapObjects reply = null;
		try{
			do{
				reply = pokemonGo.getMap().getMapObjects();
				Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
	            if (reply == null || reply.getPokestops().size() == 0) {
	            	logger.info("获取补给站信息失败,正在重试...");
	                Thread.sleep(10L * 1000);
	            }
			}while(reply == null || reply.getPokestops().size() == 0);
			logger.info("获取到周边 " + reply.getPokestops().size() + " 个补给站信息");
		}catch (Exception e) {
			logger.error("获取补给站信息失败",e);
		}
		
		logger.info("开始启动Bot线程...");
		
		int maxThread = Integer.parseInt(Config.getProperty("bot_thread_max")) >= 1 && Integer.parseInt(Config.getProperty("bot_thread_max")) <= 5 ? Integer.parseInt(Config.getProperty("bot_thread_max")) : 1;
		ExecutorService fixedThreadPool = Executors.newFixedThreadPool(maxThread);
		
		ThreadCount threadCount = new ThreadCount();
		
		if(Boolean.parseBoolean(Config.getProperty("bot_update_profile"))){
			new Thread(new UpdateProfile(pokemonGo,threadCount)).start();
		}
		
		while(true){
			try{
				if(Boolean.parseBoolean(Config.getProperty("bot_keepalive"))){fixedThreadPool.execute(new GetMapRandomDirection(pokemonGo,threadCount));}
				if(Boolean.parseBoolean(Config.getProperty("bot_loot_pokestop"))){fixedThreadPool.execute(new LootNearbyPokestop(pokemonGo,reply.getPokestops(),threadCount));}
				if(Boolean.parseBoolean(Config.getProperty("bot_catch_pokemon"))){fixedThreadPool.execute(new CatchNearbyPokemon(pokemonGo,threadCount));}
				if(Boolean.parseBoolean(Config.getProperty("bot_release_pokemon"))){fixedThreadPool.execute(new ReleasePokemon(pokemonGo,threadCount));}
				if(Boolean.parseBoolean(Config.getProperty("bot_drop_item"))){fixedThreadPool.execute(new DropUselessItems(pokemonGo,threadCount));}
				if(Boolean.parseBoolean(Config.getProperty("bot_hatch_egg"))){fixedThreadPool.execute(new HatchEggs(pokemonGo,threadCount));}
				if(Integer.parseInt(Config.getProperty("bot_thread_await")) >= 1){
					Thread.sleep(Long.parseLong(Config.getProperty("bot_thread_await")));
				}else{
					while(threadCount.getRunThreadCount()!=0){
						Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
					}
				}
			}catch (Exception e) {
				logger.error("???",e);
			}
		}
	}
	public class ComparatorPM implements Comparator<Object>{
		@Override
		public int compare(Object pm1, Object pm2) {
			Pokemon pokemon1 = (Pokemon)pm1;
			Pokemon pokemon2 = (Pokemon)pm2;
			return pokemon1.getPokemonId().name().compareTo(pokemon2.getPokemonId().name());
		}
	}
}

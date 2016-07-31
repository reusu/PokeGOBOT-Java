package name.teemo.pogo.task;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.hisunsray.commons.res.Config;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.EggIncubator;
import com.pokegoapi.api.inventory.Inventories;
import com.pokegoapi.api.player.PlayerProfile;
import com.pokegoapi.api.pokemon.EggPokemon;
import com.pokegoapi.api.pokemon.HatchedEgg;
import com.pokegoapi.api.pokemon.Pokemon;

import POGOProtos.Networking.Responses.UseItemEggIncubatorResponseOuterClass;
import POGOProtos.Networking.Responses.UseItemEggIncubatorResponseOuterClass.UseItemEggIncubatorResponse.Result;
import name.teemo.pogo.utils.PokemonName;
import name.teemo.pogo.utils.ThreadCount;

public class HatchEggs implements Runnable{
	private static Logger logger = Logger.getLogger(HatchEggs.class);
	
	private PokemonGo pokemonGo;
	private ThreadCount threadCount;
	
	public HatchEggs(PokemonGo pokemonGo, ThreadCount threadCount) {
		this.pokemonGo = pokemonGo;
		this.threadCount = threadCount;
		threadCount.setRunThreadCount(threadCount.getRunThreadCount() + 1);
	}

	@Override
	public void run() {
		logger.debug("进入孵蛋进程");
		try{
			PlayerProfile playerProfile = null;
			Inventories inventories = null;
			
			while (playerProfile == null || inventories == null) {
				try{
					playerProfile = pokemonGo.getPlayerProfile();
					Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
					inventories = pokemonGo.getInventories();
					Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
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
			
			List<HatchedEgg> hatchedEggs = inventories.getHatchery().queryHatchedEggs();
			DecimalFormat decimalFormat = new DecimalFormat("#.##");   
			
			if(hatchedEggs.size()>0){
				pokemonGo.getInventories().updateInventories();
				Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
				for(HatchedEgg hatchedEgg : hatchedEggs){
					Pokemon newPokemon = pokemonGo.getInventories().getPokebank().getPokemonById(hatchedEgg.getId());
					Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
					String stats = "获得"+hatchedEgg.getCandy()+"糖果 获得"+hatchedEgg.getExperience()+"经验 获得" + hatchedEgg.getStardust()+"星辰";
					threadCount.setGetExperience(threadCount.getGetExperience() + hatchedEgg.getExperience());
					if (newPokemon == null) {
						logger.info("孵化宝可梦 " + stats);
	                }else{
	                	logger.info("孵化" + PokemonName.getPokemonName(newPokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")) + " - " + newPokemon.getCp() +"CP " +
	                            " - " + Double.parseDouble(decimalFormat.format(newPokemon.getIvRatio()))*100 + "% IV " + stats);
	                }
				}
			}
			
			List<EggIncubator> incubators = inventories.getIncubators();
			Iterator<EggIncubator> incubatorsIter = incubators.iterator();
			while(incubatorsIter.hasNext()){
				if(incubatorsIter.next().isInUse()){
					incubatorsIter.remove();
				}
			}
			Set<EggPokemon> eggs = inventories.getHatchery().getEggs();
			Iterator<EggPokemon> eggsIter = eggs.iterator();
			while(eggsIter.hasNext()){
				if(eggsIter.next().isIncubate()){
					eggsIter.remove();
				}
			}
			
			if(incubators.size()>0 && eggs.size()>0){
				Result result = eggs.iterator().next().incubate(incubators.get(0));
				if(result == UseItemEggIncubatorResponseOuterClass.UseItemEggIncubatorResponse.Result.SUCCESS){
					logger.info("将" + eggs.iterator().next().getEggKmWalkedTarget() +"KM的蛋蛋放进了孵蛋器");
				}else{
					logger.info("将蛋放进孵蛋器的过程中出现了错误：" + result);
				}
				Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
			}
			
		}catch (Exception e) {
			logger.error("孵蛋进程发生错误",e);
		}finally {
			threadCount.setRunThreadCount(threadCount.getRunThreadCount()-1);
			logger.debug("退出孵蛋进程");
		}
	}
}

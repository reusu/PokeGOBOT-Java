package name.teemo.pogo.task;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.hisunsray.commons.res.Config;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.Item;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.EncounterResult;

import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId;
import POGOProtos.Networking.Responses.EncounterResponseOuterClass.EncounterResponse.Status;
import name.teemo.pogo.utils.PokemonName;
import name.teemo.pogo.utils.ThreadCount;
import POGOProtos.Networking.Responses.CatchPokemonResponseOuterClass.CatchPokemonResponse;

public class CatchNearbyPokemon implements Runnable{
	private static Logger logger = Logger.getLogger(CatchNearbyPokemon.class);
	
	private PokemonGo pokemonGo;
	private ThreadCount threadCount;
	
	public CatchNearbyPokemon(PokemonGo pokemonGo, ThreadCount threadCount) {
		this.pokemonGo = pokemonGo;
		this.threadCount = threadCount;
		threadCount.setRunThreadCount(threadCount.getRunThreadCount() + 1);
	}

	@Override
	public void run() {
		try{
			Collection<Item> items = pokemonGo.getInventories().getItemBag().getItems();
			int ballTypeCount = 0;
			Iterator<Item> itemsIter = items.iterator();
			while(itemsIter.hasNext()){
				Item item = itemsIter.next();
				if((item.getItemId() == ItemId.ITEM_POKE_BALL
					||item.getItemId() == ItemId.ITEM_GREAT_BALL
					||item.getItemId() == ItemId.ITEM_ULTRA_BALL
					||item.getItemId() == ItemId.ITEM_MASTER_BALL) 
					&& item.getCount() > 0){
					ballTypeCount++;
				}
			}
			
			if(ballTypeCount == 0){
				logger.info("背包中无剩余精灵球，跳过捕捉进程");
			}else{
				
				List<CatchablePokemon> catchablePokemons = pokemonGo.getMap().getCatchablePokemon();
				logger.info("检索到周边有可以抓的宝可梦 " + catchablePokemons.size() + " 只");
				if(catchablePokemons.size()>0){
					Iterator<CatchablePokemon> catchablePokemonIter = catchablePokemons.iterator();
					while(catchablePokemonIter.hasNext()){
						CatchablePokemon catchablePokemon = catchablePokemonIter.next();
						if(Config.getProperty("ignored_pokemon").contains(catchablePokemon.getPokemonId().name())){
							logger.info("宝可梦 " + catchablePokemon.getPokemonId().name() + " 因为黑名单的设置被无视");
						}else{
							logger.info("野生的宝可梦 " + PokemonName.getPokemonName(catchablePokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")) + " 从草里跳出来!");
							pokemonGo.setLocation(catchablePokemon.getLatitude(), catchablePokemon.getLongitude(), 0.0);
							Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
							EncounterResult encounterResult  = catchablePokemon.encounterPokemon();
							if (encounterResult.wasSuccessful()) {
								logger.info("遇到宝可梦 " + PokemonName.getPokemonName(catchablePokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")) + " CP " + encounterResult.getWildPokemon().getPokemonData().getCp());
								Collection<Item> _items = pokemonGo.getInventories().getItemBag().getItems();
								Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
								int _ballTypeCount = 0;
								int _barryCount = 0;
								Iterator<Item> _itemsIter = _items.iterator();
								while(_itemsIter.hasNext()){
									Item _item = _itemsIter.next();
									if((_item.getItemId() == ItemId.ITEM_POKE_BALL
											||_item.getItemId() == ItemId.ITEM_GREAT_BALL
											||_item.getItemId() == ItemId.ITEM_ULTRA_BALL
											||_item.getItemId() == ItemId.ITEM_MASTER_BALL) 
											&& _item.getCount() > 0){
										_ballTypeCount++;
									}
									if(_item.getItemId() == ItemId.ITEM_RAZZ_BERRY && _item.getCount() > 0){
										_barryCount++;
									}
								}
								if(_ballTypeCount==0){
									logger.info("背包中无剩余精灵球，跳过捕捉进程");
								}else{
									logger.info("去吧！精灵球！");
									CatchResult catchResult = _barryCount!=0?catchablePokemon.catchPokemonWithRazzBerry():catchablePokemon.catchPokemon();
					                if (catchResult.getStatus() == CatchPokemonResponse.CatchStatus.CATCH_SUCCESS) {
					                    double iv = ((double)encounterResult.getWildPokemon().getPokemonData().getIndividualAttack() 
					                    		+ (double)encounterResult.getWildPokemon().getPokemonData().getIndividualDefense() 
					                    		+ encounterResult.getWildPokemon().getPokemonData().getIndividualStamina()) * 100 / 45;
					                    DecimalFormat decimalFormat = new DecimalFormat("#.##");   
					                    logger.info("抓住了一只 " + PokemonName.getPokemonName(catchablePokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")) + " CP " + Double.parseDouble(decimalFormat.format(iv)) + "% IV " + iv);
					                    logger.info("获得了 " + catchResult.getXpList().get(0) + " 经验值 " + catchResult.getCandyList().get(0)  + " 糖果 " + catchResult.getStardustList().get(0)  + " 星辰");
					                    threadCount.setCatchPokemonCount(threadCount.getCatchPokemonCount() + 1);
					                    threadCount.setGetExperience(threadCount.getGetExperience() + catchResult.getXpList().get(0));
					                Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
					                }else{
					                	logger.info("捕捉失败，原因: " + encounterResult.getStatus());
					                }
								}
				                Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
						        
							}else{
								logger.info("捕捉失败，原因: " + encounterResult.getStatus());
								if (encounterResult.getStatus() == Status.POKEMON_INVENTORY_FULL) {
									logger.info("野生的宝可梦逃走了...");
				                }
							}
						}
					}
				}
			}
			
		}catch (Exception e) {
			logger.error("精灵捕捉进程发生错误",e);
		}finally {
			threadCount.setRunThreadCount(threadCount.getRunThreadCount()-1);
		}
	}
}

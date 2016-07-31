package name.teemo.pogo.task;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.hisunsray.commons.res.Config;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.pokemon.Pokemon;

import name.teemo.pogo.utils.PokemonName;
import name.teemo.pogo.utils.ThreadCount;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result;

public class ReleasePokemon implements Runnable{
	private static Logger logger = Logger.getLogger(ReleasePokemon.class);
	
	private PokemonGo pokemonGo;
	private ThreadCount threadCount;
	
	public ReleasePokemon(PokemonGo pokemonGo, ThreadCount threadCount) {
		this.pokemonGo = pokemonGo;
		this.threadCount = threadCount;
		threadCount.setRunThreadCount(threadCount.getRunThreadCount() + 1);
	}
	@Override
	public void run() {
		try{
			List<Pokemon> pokemons = pokemonGo.getInventories().getPokebank().getPokemons();
			Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
			Iterator<Pokemon> pokemonsIter = pokemons.iterator();
			while(pokemonsIter.hasNext()){
				Pokemon pokemon = pokemonsIter.next();
				double pokemonIV = pokemon.getIvRatio();
				int pokemonCP = pokemon.getCp();
				if (Config.getProperty("obligatory_transfer").contains(PokemonName.getPokemonName(pokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")))) {
					logger.info("正在放生宝可梦 " + PokemonName.getPokemonName(pokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")) + " 原因:强制放生");
					Result releasePokemonResult = pokemon.transferPokemon();
					Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
					if(releasePokemonResult == Result.SUCCESS){
						logger.info("已放生宝可梦 " + PokemonName.getPokemonName(pokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")));
						threadCount.setReleasePokemonCount(threadCount.getReleasePokemonCount() + 1);
					}else{
						logger.info("放生宝可梦 " + PokemonName.getPokemonName(pokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")) + " 失败");
					}
				}else if(pokemonIV*100 < Integer.parseInt(Config.getProperty("transfer_iv_threshold")) && Integer.parseInt(Config.getProperty("transfer_iv_threshold")) > 0){
					logger.info("正在放生宝可梦 " + PokemonName.getPokemonName(pokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")) + " 原因 " + (pokemonIV*100) + " IV不达标");
					Result releasePokemonResult = pokemon.transferPokemon();
					Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
					if(releasePokemonResult == Result.SUCCESS){
						logger.info("已放生宝可梦 " + PokemonName.getPokemonName(pokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")));
						threadCount.setReleasePokemonCount(threadCount.getReleasePokemonCount() + 1);
					}else{
						logger.info("放生宝可梦 " + PokemonName.getPokemonName(pokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")) + " 失败");
					}
				}else if(pokemonCP < Integer.parseInt(Config.getProperty("transfer_cp_threshold")) && Integer.parseInt(Config.getProperty("transfer_cp_threshold")) > 0){
					logger.info("正在放生宝可梦 " + PokemonName.getPokemonName(pokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")) + " 原因:CP " + pokemonCP + " 不达标");
					Result releasePokemonResult = pokemon.transferPokemon();
					Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
					if(releasePokemonResult == Result.SUCCESS){
						logger.info("已放生宝可梦 " + PokemonName.getPokemonName(pokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")));
						threadCount.setReleasePokemonCount(threadCount.getReleasePokemonCount() + 1);
					}else{
						logger.info("放生宝可梦 " + PokemonName.getPokemonName(pokemon.getPokemonId().name(), Config.getProperty("pokemon_lang")) + " 失败");
					}
				}
			}
		}catch (Exception e) {
			logger.error("精灵放生进程发生错误",e);
		}finally {
			threadCount.setRunThreadCount(threadCount.getRunThreadCount()-1);
		}
	}
}

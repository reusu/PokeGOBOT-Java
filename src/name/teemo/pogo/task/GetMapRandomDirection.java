package name.teemo.pogo.task;

import org.apache.log4j.Logger;

import com.hisunsray.commons.res.Config;
import com.pokegoapi.api.PokemonGo;

import name.teemo.pogo.utils.ThreadCount;

public class GetMapRandomDirection implements Runnable{
	private static Logger logger = Logger.getLogger(GetMapRandomDirection.class);
	
	private PokemonGo pokemonGo;
	private ThreadCount threadCount;
	
	public GetMapRandomDirection(PokemonGo pokemonGo, ThreadCount threadCount) {
		this.pokemonGo = pokemonGo;
		this.threadCount = threadCount;
		threadCount.setRunThreadCount(threadCount.getRunThreadCount() + 1);
	}

	@Override
	public void run() {
		logger.debug("进入探索进程");
		try{
			if(!threadCount.getWaking()){
				Double lat = pokemonGo.getLatitude() + randomLatLng();
				Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
				Double lng = pokemonGo.getLongitude() + randomLatLng();
				Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
				logger.info("探索地图信息 (" + lat + ", " + lng + ")");
				pokemonGo.setLocation(lat, lng, 0.0);
				Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
			}
		}catch (Exception e) {
			logger.error("探索地图进程发生错误",e);
		}finally {
			threadCount.setRunThreadCount(threadCount.getRunThreadCount()-1);
			logger.debug("退出探索进程");
		}
	}
	
    private Double randomLatLng(){
        return Math.random() * 0.0001 - 0.00005;
    }
}

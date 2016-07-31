package name.teemo.pogo.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.hisunsray.commons.res.Config;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.google.common.geometry.S2LatLng;

import POGOProtos.Inventory.Item.ItemAwardOuterClass.ItemAward;
import POGOProtos.Networking.Responses.FortSearchResponseOuterClass.FortSearchResponse.Result;
import name.teemo.pogo.utils.ThreadCount;

public class LootNearbyPokestop implements Runnable{
	
	private static Logger logger = Logger.getLogger(LootNearbyPokestop.class);
	
	private PokemonGo pokemonGo;
	private ThreadCount threadCount;
	private Collection<Pokestop> pokestops;
	public LootNearbyPokestop(PokemonGo pokemonGo, Collection<Pokestop> pokestops, ThreadCount threadCount) {
		this.pokemonGo = pokemonGo;
		this.pokestops = pokestops;
		this.threadCount = threadCount;
		threadCount.setRunThreadCount(threadCount.getRunThreadCount() + 1);
	}
	
	@Override
	public void run() {
		try{
			List<Pokestop> pokestopList = new ArrayList<Pokestop>();
			Iterator<Pokestop> pokestopsIter = pokestops.iterator();
			while(pokestopsIter.hasNext()){
				pokestopList.add(pokestopsIter.next());
			}
			ComparatorLng comparatorLng = new ComparatorLng();
			Collections.sort(pokestopList,comparatorLng);
			Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
			
			Iterator<Pokestop> pokeStopListIter = pokestopList.iterator();
			while(pokeStopListIter.hasNext()){
				Pokestop pokestop = pokeStopListIter.next();
				if(pokestop.canLoot() && pokestop.inRange()){
					String pokestopId = pokestop.getId();
					logger.info("正在滑动补给站" + pokestopId);
					PokestopLootResult result = pokestop.loot();
					Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
					if(result.getResult() == Result.SUCCESS){
						logger.info("成功的摸了一把补给站 " + pokestopId + "，获得了 " + result.getExperience() + " 经验值");
						List<ItemAward> itemAwadeds = result.getItemsAwarded();
						for(ItemAward itemAwaded : itemAwadeds){
							logger.info("获得了 " + itemAwaded.getItemCount() + " 个 " + itemAwaded.getItemId());
							threadCount.setGetPokestopItem(threadCount.getGetPokestopItem() + itemAwaded.getItemCount());
						}
						threadCount.setGetExperience(threadCount.getGetExperience() + result.getExperience());
					}else if(result.getResult() == Result.INVENTORY_FULL){
						logger.info("道具已满");
					}else if(result.getResult() == Result.OUT_OF_RANGE){
						logger.info("距离补给站太远了");
					}else if(result.getResult() == Result.IN_COOLDOWN_PERIOD){
						logger.info("这是刚摸过的补给站");
					}else if(result.getResult() == Result.NO_RESULT_SET ){
						logger.info("未知异常");
					}
				}else if(!pokestop.inRange() && !threadCount.getWaking()){
					logger.info("移动到较远的补给站");
					S2LatLng target = S2LatLng.fromDegrees(pokestop.getLatitude(), pokestop.getLongitude());
					walk(target);
					break;
				}
			}
		}catch (Exception e) {
			logger.error("补给进程发生错误",e);
		}finally {
			threadCount.setRunThreadCount(threadCount.getRunThreadCount()-1);
		}
	}
	
	public class ComparatorLng implements Comparator<Object>{
		@Override
		public int compare(Object pokes1, Object pokes2) {
			Pokestop pokeStops1 = (Pokestop)pokes1;
			Pokestop pokeStops2 = (Pokestop)pokes2;
			S2LatLng locationA = S2LatLng.fromDegrees(pokeStops1.getLatitude(), pokeStops1.getLongitude());
			S2LatLng locationB = S2LatLng.fromDegrees(pokeStops2.getLatitude(), pokeStops2.getLongitude());
			S2LatLng self = S2LatLng.fromDegrees(pokemonGo.getLatitude(), pokemonGo.getLongitude());
			Double distanceA = self.getEarthDistance(locationA);
			Double distanceB = self.getEarthDistance(locationB);
			return distanceA.compareTo(distanceB);
		}
	}
	
	private void walk(S2LatLng end) throws InterruptedException{
		S2LatLng start = S2LatLng.fromDegrees(pokemonGo.getLatitude(), pokemonGo.getLongitude());
		S2LatLng diff = end.sub(start);
		double distance = start.getEarthDistance(end);
		double speed = Double.parseDouble(Config.getProperty("bot_walk_speed"));
		double timeRequired = distance / speed;
		long timeout = 350L;
		double stepsRequired = timeRequired / ((double)timeout / (double)1000);
		if(stepsRequired==0){
		}else{
			double deltaLat = diff.latDegrees() / stepsRequired;
			double deltaLng = diff.lngDegrees() / stepsRequired;
			logger.info("正在移动到目标 " + end.toStringDegrees() + " 需要步数 " + stepsRequired  + "步");
			threadCount.setWaking(true);
			for(int i=0;i<=(int)stepsRequired;i++){
				S2LatLng now = S2LatLng.fromDegrees(pokemonGo.getLatitude(), pokemonGo.getLongitude());
//				logger.info("当前坐标 (" + now.latDegrees() + "," + now.lngDegrees() + ")");
				pokemonGo.setLocation(now.latDegrees()+ deltaLat, now.lngDegrees() + deltaLng, 0.0);
				Thread.sleep(timeout);
			}
		}
	}
}

package name.teemo.pogo.task;

import org.apache.log4j.Logger;

import com.hisunsray.commons.res.Config;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.Item;
import com.pokegoapi.api.inventory.ItemBag;

import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId;
import POGOProtos.Networking.Responses.RecycleInventoryItemResponseOuterClass.RecycleInventoryItemResponse.Result;
import name.teemo.pogo.utils.ThreadCount;

public class DropUselessItems implements Runnable{
	private static Logger logger = Logger.getLogger(DropUselessItems.class);
	
	private PokemonGo pokemonGo;
	private ThreadCount threadCount;
	
	public DropUselessItems(PokemonGo pokemonGo, ThreadCount threadCount) {
		this.pokemonGo = pokemonGo;
		this.threadCount = threadCount;
		threadCount.setRunThreadCount(threadCount.getRunThreadCount() + 1);
	}

	private static ItemId[] dItem = {
			ItemId.ITEM_REVIVE, ItemId.ITEM_MAX_REVIVE, ItemId.ITEM_POTION,
			ItemId.ITEM_SUPER_POTION, ItemId.ITEM_HYPER_POTION, ItemId.ITEM_MAX_POTION,
			ItemId.ITEM_POKE_BALL, ItemId.ITEM_GREAT_BALL, ItemId.ITEM_ULTRA_BALL,
			ItemId.ITEM_MASTER_BALL, ItemId.ITEM_RAZZ_BERRY, ItemId.ITEM_LUCKY_EGG,
			ItemId.ITEM_INCENSE_ORDINARY, ItemId.ITEM_TROY_DISK};
	
	@Override
	public void run() {
		logger.debug("进入丢弃进程");
		try{
			int[] dItemMax = {
					Integer.parseInt(Config.getProperty("item_revive")), Integer.parseInt(Config.getProperty("item_max_revive")), Integer.parseInt(Config.getProperty("item_potion")),
					Integer.parseInt(Config.getProperty("item_super_potion")), Integer.parseInt(Config.getProperty("item_hyper_potion")), Integer.parseInt(Config.getProperty("item_max_potion")),
					Integer.parseInt(Config.getProperty("item_poke_ball")), Integer.parseInt(Config.getProperty("item_great_ball")), Integer.parseInt(Config.getProperty("item_ultra_ball")),
					Integer.parseInt(Config.getProperty("item_master_ball")), Integer.parseInt(Config.getProperty("item_razz_berry"))};
			ItemBag itemBag = pokemonGo.getInventories().getItemBag();
			Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
			for(int i = 0;i < dItemMax.length ;i++){
				Item item = itemBag.getItem(dItem[i]);
				int count = item.getCount() - dItemMax[i];
				if(dItemMax[i]!=-1 && count > 0){
					Result result = pokemonGo.getInventories().getItemBag().removeItem(dItem[i], count);
					Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
					if(result == Result.SUCCESS){
						logger.info("丢弃了 " + count + " 个 " + dItem[i].name());
						threadCount.setDropPokestopItem(threadCount.getDropPokestopItem() + 1 );
					}else{
						logger.info("丢弃了 " + count + " 个 " + dItem[i].name() + "的时候失败了！");
					}
				}
			}
		}catch (Exception e) {
			logger.error("丢弃道具进程发生错误",e);
		}finally {
			threadCount.setRunThreadCount(threadCount.getRunThreadCount()-1);
			logger.debug("退出丢弃进程");
		}
	}
}

package name.teemo.pogo.task;

import java.text.DecimalFormat;

import org.apache.log4j.Logger;

import com.hisunsray.commons.res.Config;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.inventory.Inventories;
import com.pokegoapi.api.player.PlayerProfile;

import name.teemo.pogo.utils.ThreadCount;

public class UpdateProfile implements Runnable{
	private static Logger logger = Logger.getLogger(UpdateProfile.class);
	
	private PokemonGo pokemonGo;
	private ThreadCount threadCount;
	
	public UpdateProfile(PokemonGo pokemonGo, ThreadCount threadCount) {
		this.pokemonGo = pokemonGo;
		this.threadCount = threadCount;
	}

	@Override
	public void run() {
		while(true){
			try{
				threadCount.setRunThreadCount(threadCount.getRunThreadCount()+1);
				
				int requiredXp[] = {
						0, 1000, 3000, 6000, 10000, 15000, 21000, 28000, 36000, 45000, 
						55000, 65000, 75000, 85000, 100000, 120000, 140000, 160000, 185000, 210000, 
						260000, 335000, 435000, 560000, 710000, 900000, 1100000, 1350000, 1650000, 2000000, 
						2500000, 3000000, 3750000, 4750000, 6000000, 7500000, 9500000, 12000000, 15000000, 20000000};
				
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
				
		        int nextXP = requiredXp[playerProfile.getStats().getLevel()] - requiredXp[playerProfile.getStats().getLevel() - 1];
		        int curLevelXP = (int)playerProfile.getStats().getExperience()  - requiredXp[playerProfile.getStats().getLevel() - 1];
		        String ratio = new DecimalFormat("#0.00").format((double)curLevelXP / (double)nextXP * 100.0);
		        
		        logger.info("更新账号信息: 当前经验值 " + playerProfile.getStats().getExperience() + " 当前等级 " + playerProfile.getStats().getLevel() + " 经验值进度 " + curLevelXP + "/" + nextXP + " (" + ratio + " %) 至 下一等级 " + (playerProfile.getStats().getLevel() + 1));
		        logger.info("Bot获得经验值 " + threadCount.getGetExperience() 
		        + " 捕捉精灵 " + threadCount.getCatchPokemonCount()  + " 放生精灵 " +threadCount.getReleasePokemonCount() 
		        + " 获得道具 " + threadCount.getGetPokestopItem() + " 丢弃道具 " + threadCount.getDropPokestopItem());
		        
		        
			}catch (Exception e) {
				logger.error("更新账号进程发生错误",e);
			}finally {
				threadCount.setRunThreadCount(threadCount.getRunThreadCount()-1);
				try {
					Thread.sleep(Long.parseLong(Config.getProperty("bot_upprofile_await")));
				} catch (Exception e) {
					logger.error("等待进程发生错误",e);
				}
			}
		}
	}
}

package name.teemo.pogo;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.hisunsray.commons.res.Config;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.GoogleAutoCredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

public class PokemonGoBotMain {
	
	private static Logger logger = Logger.getLogger(PokemonGoBotMain.class);

	public static void main(String[] args) throws Exception {
		Builder httpBuilder = new OkHttpClient().newBuilder();
		httpBuilder.connectTimeout(120, TimeUnit.SECONDS);
		httpBuilder.readTimeout(60, TimeUnit.SECONDS);
		httpBuilder.writeTimeout(60, TimeUnit.SECONDS);
		OkHttpClient httpClient = httpBuilder.build();
		
		Config.setConfigResource(Class.class.getClass().getResource("/").getPath() + "config.properties");
		
		logger.info("正在登录服务器...");
		
		int retryCount = 3;
		int tryCount = 1;
		long errorTimeout = 1000L;
		
		PokemonGo pokemonGo = null;
		do{
			try{
				pokemonGo = new PokemonGo(getAuth(httpClient), httpClient);
			}catch (LoginFailedException loginFailedException) {
				logger.error("服务器拒绝账户登录，请检查账户信息...");
				System.exit(1);
			}catch (RemoteServerException remoteServerException) {
				logger.error("服务器返回了一个错误 ，等待重试...");
				try{
					Thread.sleep(errorTimeout);
				}catch (Exception e) {
					logger.error("系统等待出现异常...");
				}
			}
		}while(pokemonGo==null && tryCount<= retryCount++);
		
		if (pokemonGo == null) {
			logger.error("登录失败，退出进程...");
	        System.exit(1);
	    }
		
		logger.info("成功登录帐号 " + Config.getProperty("username"));
		
		pokemonGo.setLocation(Double.parseDouble(Config.getProperty("latitude")), Double.parseDouble(Config.getProperty("longitude")), 0.0);
		Thread.sleep(Long.parseLong(Config.getProperty("api_loop_await")));
		logger.info("初始化空降坐标 (" + Config.getProperty("latitude") + ", " + Config.getProperty("longitude") + ")");
		
		PokemonGoBot pokemonGoBot = new PokemonGoBot(pokemonGo);
		Thread pokemonGoBotThread = new Thread(pokemonGoBot);
		pokemonGoBotThread.start();
	}
	
	private static CredentialProvider getAuth(OkHttpClient http) throws LoginFailedException, RemoteServerException{
		String username = Config.getProperty("username");
		String password = Config.getProperty("password");
		return username.contains("@")?new GoogleAutoCredentialProvider(http,username,password):new PtcCredentialProvider(http,username,password);
	}

}

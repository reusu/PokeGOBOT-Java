package name.teemo.pogo.utils;

import java.util.Calendar;

public class ThreadCount {
	private Integer runThreadCount;
	private Integer catchPokemonCount;
	private Integer releasePokemonCount;
	private Integer getPokestopItem;
	private Integer dropPokestopItem;
	private Integer getExperience;
	private Boolean waking;
	private Long upWalkTime;
	public ThreadCount() {
		runThreadCount = 0;
		catchPokemonCount = 0;
		releasePokemonCount = 0;
		getPokestopItem = 0;
		dropPokestopItem = 0;
		getExperience = 0;
		waking = false;
		upWalkTime = Calendar.getInstance().getTimeInMillis();
	}

	public Integer getRunThreadCount() {
		return runThreadCount;
	}

	public void setRunThreadCount(Integer runThreadCount) {
		this.runThreadCount = runThreadCount;
	}

	public Integer getCatchPokemonCount() {
		return catchPokemonCount;
	}

	public void setCatchPokemonCount(Integer catchPokemonCount) {
		this.catchPokemonCount = catchPokemonCount;
	}

	public Integer getReleasePokemonCount() {
		return releasePokemonCount;
	}

	public void setReleasePokemonCount(Integer releasePokemonCount) {
		this.releasePokemonCount = releasePokemonCount;
	}

	public Integer getGetPokestopItem() {
		return getPokestopItem;
	}

	public void setGetPokestopItem(Integer getPokestopItem) {
		this.getPokestopItem = getPokestopItem;
	}

	public Integer getDropPokestopItem() {
		return dropPokestopItem;
	}

	public void setDropPokestopItem(Integer dropPokestopItem) {
		this.dropPokestopItem = dropPokestopItem;
	}

	public Integer getGetExperience() {
		return getExperience;
	}

	public void setGetExperience(Integer getExperience) {
		this.getExperience = getExperience;
	}

	public Boolean getWaking() {
		return waking;
	}

	public void setWaking(Boolean waking) {
		this.waking = waking;
	}

	public Long getUpWalkTime() {
		return upWalkTime;
	}

	public void setUpWalkTime(Long upWalkTime) {
		this.upWalkTime = upWalkTime;
	}	

	
}

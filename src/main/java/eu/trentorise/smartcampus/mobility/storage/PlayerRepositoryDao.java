package eu.trentorise.smartcampus.mobility.storage;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;

@Repository
public interface PlayerRepositoryDao extends CrudRepository<Player, String>{
	
	/*public Player findByPid(String id);
	
	public Player findBySocialId(String id);

	@Query("{'nickName': ?0}")
	public Player findByNick(String nickname);
	
	@Query("{'nickName': { '$regex': ?0, $options:'i'}}")
	public Player findByNickIgnoreCase(String nickname);*/
	
	Iterable<Player> findAllByType(String type);
	
	Iterable<Player> findAllByTypeAndCheckedRecommendation(String type, boolean recommendation);
	
	public Player findByPidAndType(String id, String type);
	
	public Player findBySocialIdAndType(String id, String type);

	@Query("{'nickname': ?0}")
	public Player findByNickAndType(String nickname, String type);
	
	@Query("{'nickname': { '$regex': ?0, $options:'i'}}")
	public Player findByNickIgnoreCaseAndType(String nickname, String type);

}

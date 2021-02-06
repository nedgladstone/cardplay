package com.github.nedgladstone.cardball.controller;

import com.github.nedgladstone.cardball.model.*;
import com.github.nedgladstone.cardball.repository.GameRepository;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ExecuteOn(TaskExecutors.IO)
@Controller("/game")
public class GameController {
    protected final TeamController teamController;
    protected final PlayerController playerController;
    protected final GameRepository gameRepository;

    public GameController(TeamController teamController, PlayerController playerController, GameRepository gameRepository) {
        this.teamController = teamController;
        this.playerController = playerController;
        this.gameRepository = gameRepository;
    }

    @Get
    public Iterable<GameStatus> list() {
        return StreamSupport.stream(gameRepository.findAll().spliterator(), false).map(Game::getStatus).collect(Collectors.toList());
    }

    @Get("/{id}")
    public GameStatus find(long id) {
        return findGame(id).getStatus();
    }

    @Post
    public void createGame(GameDefinition definition) {
        Team visitingTeam = findTeam(definition.getVisitingTeamId());
        Team homeTeam = findTeam(definition.getHomeTeamId());
        Game game = new Game(visitingTeam, homeTeam);
        gameRepository.save(game);
    }

    @Put("/{gameId}/lineup/{side}")
    public GameStatus putLineup(long gameId, String side, LineupDefinition lineupDefinition) {
        Game game = findGame(gameId);
        List<Participant> lineup = lineupDefinition.getParticipants().stream()
                .map(pd -> new Participant(
                        game,
                        pd.getNumberInBattingOrder(),
                        pd.getFieldingPosition(),
                        playerController.find(pd.getPlayerId())))
                .collect(Collectors.toList());
        game.putLineup(Game.Side.fromString(side), lineup);
        gameRepository.save(game);
        return game.getStatus();
    }

    @Post("/{gameId}/strategy/{role}")
    public GameStatus postStrategy(long gameId, String role, String type, @Body String strategy) {
        Game game = findGame(gameId);
        game.postStrategy(Game.Role.fromString(role), strategy);
        gameRepository.save(game);
        return game.getStatus();
    }

    @Get("/sneaky")
    public String sneaky() {
        Team rockies = new Team(new TeamDefinition("Colorado", "Rockies", "Ned", "Gladstone"))
                .addPlayer(new Player("Todd", "Helton", 2003, 3, null, 'R', 'R', 308, 999))
                .addPlayer(new Player("Larry", "Walker", 1998, 9, null, 'L', 'L', 297, 999));
        Team phillies = new Team(new TeamDefinition("Philadelphia", "Phillies", "Ed", "Gladstone"))
                .addPlayer(new Player("Greg", "Luzinski", 1978, 7, null, 'R', 'R', 276, 999))
                .addPlayer(new Player("Larry", "Bowa", 1980, 6, null, 'R', 'R', 266, 999));
        Game game7 = new Game(phillies, rockies)
                .addAction(new Action(null, null, null, 1, 0, 0, phillies.getPlayers().get(0), new Timestamp(System.currentTimeMillis()), 0, 0, "KL", "", 0, false, true)
                        .addResult(new Action(null, null, null, 0, 0, 0, phillies.getPlayers().get(1), new Timestamp(System.currentTimeMillis()), 1, 2, "PB", "", 2, false, false)));
        gameRepository.save(game7);
        return "Go Rox!";
    }

    private Game findGame(long id) {
        Optional<Game> gameOptional = gameRepository.findById(id);
        if (gameOptional.isEmpty()) {
            throw new IllegalArgumentException("Game " + id + " does not exist");
        }
        return gameOptional.get();
    }

    private Team findTeam(long teamId) {
        return teamController.find(teamId);
    }
}

""" Run Java's Market-Sim. """
import copy
import os
import os.path as osp
import tempfile

import gin
import numpy.random as rand
import ujson as json


@gin.configurable
class MarketSimulator:
    """Runs the Java Market-Sim."""

    def __init__(
        self,
        market_sim_dir: str,
        agent_strategy_name: str,
        agent_omega_depth: int,
        agent_arrival_rate: float,
        background_name: str,
        background_strategy_name: str,
        background_probability: list,
        background_number: int,
        market_maker_exists: bool,
        market_maker_name: str,
        market_maker_strategy_name: str,
        market_config: dict,
    ):
        """Constructor

        Args:
            market_sim_dir: Directory containing the Market-Sim run script `market-sim.sh`.
            agent_strategy_name: The name of the strategy to be implemented by the agent, typically `tfrl`.
            agent_omega_depth: The number of trades considered when the agent calculates the omega ratio in Market-Sim.
            background_name: The role name of the background agents.
            background_strategy_name: A list of strategy names for background traders, to find strategies,
                                        run `./market-sim -a` from Market-Sim.
            background_probability: A list of the probability of each background strategy being played,
                                    the index of a probability should be the same as its corresponding strategy.
            background_number: The number of background agents to initialize in Market-Sim.
            market_maker_exists: Boolean indicating if a market maker exists in the market.
            market_maker_name: The role name of the market maker agent.
            market_maker_strategy_name: The strategy name for the market maker.
            background_probability: A list of the probability of each background strategy being played,
            market_config: Configuration dictionary for Market-Sim.
        """
        self.market_sim_dir = market_sim_dir

        self.agent_strategy_name = agent_strategy_name
        self.agent_omega_depth = agent_omega_depth
        self.agent_arrival_rate = agent_arrival_rate

        self.background_name = background_name
        self.background_strategy_name = background_strategy_name
        self.background_probability = background_probability
        self.background_number = background_number

        self.market_maker_exists = market_maker_exists
        self.market_maker_name = market_maker_name
        self.market_maker_strategy_name = market_maker_strategy_name

        self.market_config = market_config

    def simulate(
        self,
        agent_name: str,
        model_path: str,
        agent_max_vector_depth: int,
        agent_additional_actions: str,
        validation_mode: bool,
        load_mode: bool,
        load_path: str,
    ):

        sim_config = {}
        market_assignment = {}

        # Add the current agent to the market assignment in market-sim config file
        agent_role = {}
        agent_strategy = (
            f"{self.agent_strategy_name}:omegaDepth_{self.agent_omega_depth}_maxVectorDepth_{agent_max_vector_depth}"
        )
        agent_strategy = agent_strategy + f"_arrivalRate_{self.agent_arrival_rate}"
        agent_role[agent_strategy] = 1
        market_assignment[agent_name] = agent_role

        # Add the background agent to the market assignment in market-sim config file
        background_dist = rand.multinomial(self.background_number, self.background_probability)
        background_role = {}
        for count, strat in enumerate(self.background_strategy_name):
            background_role[strat] = int(background_dist[count])
        market_assignment[self.background_name] = background_role

        # Add the market maker agent to the market assignment in market-sim config file
        if self.market_maker_exists:
            market_assignment[self.market_maker_name] = {self.market_maker_strategy_name: 1}

        sim_config["assignment"] = market_assignment

        # Add the market configuration to the market-sim config file
        market_config = copy.deepcopy(self.market_config)
        market_config["TensorFlowModelPath"] = model_path
        market_config["AdditionalActions"] = agent_additional_actions
        market_config["IsTraining"] = not validation_mode
        sim_config["configuration"] = market_config

        # Save market-sim config to a temporary file so it can be loaded from disk.
        temp_dir = tempfile.TemporaryDirectory()
        config_path = osp.join(temp_dir.name, "market_sim_config.json")
        json.dump(sim_config, open(config_path, "w"), sort_keys=True)

        # Run market-sim.
        output_path_local_train = osp.join(model_path, "output_local.json")
        output_path_local_val = osp.join(model_path, "output_local_val.json")
        output_path_local_load = osp.join(load_path, "output_local.json")

        if validation_mode:
            output_path_local = output_path_local_val
        elif load_mode:
            output_path_local = output_path_local_load
        else:
            output_path_local = output_path_local_train

        if load_mode:
            error_path = osp.join(load_path, "error_local.txt")
        else:
            error_path = osp.join(model_path, "error_local.txt")
        # Add back 2>{error_path}
        print(f"java -jar {self.market_sim_dir} --spec {config_path} > {output_path_local} 2>{error_path}")
        os.system(f"java -jar {self.market_sim_dir} --spec {config_path} > {output_path_local} 2>{error_path}")

        simulation_results = json.load(open(output_path_local, "r"))

        temp_dir.cleanup()
        return simulation_results


@gin.configurable
class DummyMarketSimulator:
    """Fake version of Market-Sim for testing."""

    def __init__(self, dummy_result_path: str):
        """Constructor.

        Args:
            dummy_result_path: Path to an simulation result that will always be returned.
        """
        self.simulation_results = json.load(open(dummy_result_path, "r"))

    def simulate(self, *args, **kwargs):
        return self.simulation_results

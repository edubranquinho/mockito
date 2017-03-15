package br.com.caelum.leilao.servico;

import java.util.Calendar;
import java.util.List;

import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.dominio.Pagamento;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import br.com.caelum.leilao.infra.dao.RepositorioDePagamentos;
import br.com.caelum.leilao.infra.relogio.Relogio;
import br.com.caelum.leilao.infra.relogio.RelogioDoSistema;

public class GeradorDePagamento {

	private RepositorioDePagamentos repositorio;
	private RepositorioDeLeiloes repositorioDeLeiloes;
	private Avaliador avaliador;
	private Relogio relogio;

	public GeradorDePagamento(RepositorioDePagamentos repositorio, RepositorioDeLeiloes repositorioDeLeiloes,
			Avaliador avaliador, Relogio relogio) {
		this.repositorio = repositorio;
		this.repositorioDeLeiloes = repositorioDeLeiloes;
		this.avaliador = avaliador;
		this.relogio = relogio;
	}

	public GeradorDePagamento(RepositorioDePagamentos repositorio, RepositorioDeLeiloes repositorioDeLeiloes,
			Avaliador avaliador) {
		this(repositorio, repositorioDeLeiloes, avaliador, new RelogioDoSistema());
	}

	public void gera() {
		List<Leilao> encerrados = repositorioDeLeiloes.encerrados();
		for (Leilao leilao : encerrados) {
			avaliador.avalia(leilao);
			Pagamento pagamento = new Pagamento(avaliador.getMaiorLance(), primeiroDiaUtil());
			repositorio.salvar(pagamento);
		}
	}

	private Calendar primeiroDiaUtil() {
		Calendar data = relogio.hoje();
		int diaDaSemana = data.get(Calendar.DAY_OF_WEEK);
		if (diaDaSemana == Calendar.SATURDAY) {
			data.add(Calendar.DAY_OF_MONTH, 2);
		} else if (diaDaSemana == Calendar.SUNDAY) {
			data.add(Calendar.DAY_OF_MONTH, 1);
		}
		return data;
	}
}

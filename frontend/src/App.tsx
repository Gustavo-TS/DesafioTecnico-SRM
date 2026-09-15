import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, type Cedente, type Liquidacao, type Moeda, type Recebivel, type Resultado, type TipoRecebivel } from './services/api'
import './styles.css'

const moedas: Moeda[] = ['BRL', 'USD']
const tipos: TipoRecebivel[] = ['DUPLICATA', 'CHEQUE']
const dinheiro = (valor: number | undefined, moeda: Moeda = 'BRL') => valor == null ? '—' : new Intl.NumberFormat('pt-BR', { style: 'currency', currency: moeda }).format(valor)

function App() {
  const [cedentes, setCedentes] = useState<Cedente[]>([])
  const [recebiveis, setRecebiveis] = useState<Recebivel[]>([])
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [liquidacoes, setLiquidacoes] = useState<Liquidacao[]>([])
  const [mensagem, setMensagem] = useState('')
  const [busy, setBusy] = useState('')
  const chaveLiquidacao = useRef<string | null>(null)

  const carregar = async () => {
    const [listaCedentes, listaRecebiveis] = await Promise.all([api.listarCedentes(), api.listarRecebiveis()])
    setCedentes(listaCedentes)
    setRecebiveis(listaRecebiveis)
  }

  const mostrarErro = (erro: unknown) => setMensagem(erro instanceof Error ? erro.message : 'Erro ao processar solicitação.')
  useEffect(() => { void carregar().catch(mostrarErro) }, [])
  const enviar = (nome: string, acao: (form: HTMLFormElement) => Promise<void>) => async (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault(); setBusy(nome); setMensagem('')
    try { await acao(evento.currentTarget) } catch (erro) { mostrarErro(erro) } finally { setBusy('') }
  }
  const campo = (name: string, label: string, type = 'text') => <label>{label}<input required name={name} type={type} /></label>
  const opcoesMoeda = moedas.map(moeda => <option key={moeda}>{moeda}</option>)
  const opcoesTipo = tipos.map(tipo => <option key={tipo}>{tipo}</option>)

  return <main>
    <header><div><small>SRM</small><h1>Credit Engine</h1></div><span>Recebíveis, câmbio e liquidação</span></header>
    {mensagem && <div className="alert">{mensagem}</div>}

    <section><h2>Simulação de recebível</h2><form onSubmit={enviar('simular', async form => {
      const d = new FormData(form)
      setResultado(await api.simular({ valorFace: Number(d.get('valorFace')), dataVencimento: String(d.get('dataVencimento')), tipo: String(d.get('tipo')) as TipoRecebivel, moedaPagamento: String(d.get('moedaPagamento')) as Moeda }))
    })}>{campo('valorFace', 'Valor de face', 'number')}{campo('dataVencimento', 'Data de vencimento', 'date')}<label>Tipo<select name="tipo">{opcoesTipo}</select></label><label>Moeda de pagamento<select name="moedaPagamento">{opcoesMoeda}</select></label><button disabled={busy === 'simular'}>Simular</button></form>{resultado && <div className="resultado"><b>Resultado da operação</b><p>Valor presente <strong>{dinheiro(resultado.valorPresente, resultado.moedaPagamento)}</strong></p><p>Deságio <strong>{dinheiro(resultado.valorDesagio, resultado.moedaPagamento)}</strong></p><p>Prazo <strong>{resultado.prazoMeses} meses</strong></p><p className="total">Valor final <strong>{dinheiro(resultado.valorFinal, resultado.moedaPagamento)}</strong></p></div>}</section>

    <div className="cols"><section><h2>Cadastro de cedente</h2><form onSubmit={enviar('cedente', async form => {
      const d = new FormData(form); await api.criarCedente({ nome: String(d.get('nome')), documento: String(d.get('documento')) }); await carregar(); setMensagem('Cedente cadastrado.')
    })}>{campo('nome', 'Nome')}{campo('documento', 'Documento')}<button disabled={busy === 'cedente'}>Cadastrar cedente</button></form></section>
      <section><h2>Cadastro de taxa de câmbio</h2><form onSubmit={enviar('cambio', async form => {
        const d = new FormData(form); await api.criarTaxaCambio({ moedaOrigem: String(d.get('origem')) as Moeda, moedaDestino: String(d.get('destino')) as Moeda, taxa: Number(d.get('taxa')), vigenteEm: new Date(String(d.get('vigenteEm'))).toISOString() }); setMensagem('Taxa de câmbio cadastrada.')
      })}><label>Origem<select name="origem">{opcoesMoeda}</select></label><label>Destino<select name="destino">{opcoesMoeda}</select></label>{campo('taxa', 'Taxa', 'number')}{campo('vigenteEm', 'Vigente em', 'datetime-local')}<button disabled={busy === 'cambio'}>Cadastrar taxa</button></form></section></div>

    <section><h2>Cadastro de recebível</h2><form onSubmit={enviar('recebivel', async form => {
      const d = new FormData(form); await api.criarRecebivel({ cedenteId: String(d.get('cedenteId')), tipo: String(d.get('tipo')) as TipoRecebivel, valorFace: Number(d.get('valorFace')), dataVencimento: String(d.get('dataVencimento')) }); await carregar(); setMensagem('Recebível cadastrado.')
    })}><label>Cedente<select required name="cedenteId"><option value="">Selecione</option>{cedentes.map(cedente => <option key={cedente.id} value={cedente.id}>{cedente.nome}</option>)}</select></label><label>Tipo<select name="tipo">{opcoesTipo}</select></label>{campo('valorFace', 'Valor de face', 'number')}{campo('dataVencimento', 'Data de vencimento', 'date')}<button disabled={busy === 'recebivel'}>Cadastrar recebível</button></form></section>

    <section><h2>Liquidação</h2><form onSubmit={enviar('liquidar', async form => {
      const d = new FormData(form); chaveLiquidacao.current ??= crypto.randomUUID()
      try { setResultado(await api.liquidar(String(d.get('recebivelId')), String(d.get('moedaPagamento')) as Moeda, chaveLiquidacao.current)); await carregar(); setMensagem('Recebível liquidado.') } finally { chaveLiquidacao.current = null }
    })}><label>Recebível<select required name="recebivelId"><option value="">Selecione</option>{recebiveis.filter(recebivel => recebivel.status === 'PENDENTE').map(recebivel => <option key={recebivel.id} value={recebivel.id}>{recebivel.tipo} · {dinheiro(recebivel.valorFace)} · {recebivel.dataVencimento}</option>)}</select></label><label>Moeda de pagamento<select name="moedaPagamento">{opcoesMoeda}</select></label><button disabled={busy === 'liquidar'}>Liquidar</button></form></section>

    <section><h2>Extrato de liquidações</h2><form onSubmit={enviar('extrato', async form => {
      const d = new FormData(form); setLiquidacoes(await api.listarLiquidacoes({ dataInicio: String(d.get('dataInicio') || ''), dataFim: String(d.get('dataFim') || ''), cedenteId: String(d.get('cedenteId') || ''), moeda: String(d.get('moeda') || '') as Moeda }))
    })}>{campo('dataInicio', 'Data inicial', 'date')}{campo('dataFim', 'Data final', 'date')}<label>Cedente<select name="cedenteId"><option value="">Todos</option>{cedentes.map(cedente => <option key={cedente.id} value={cedente.id}>{cedente.nome}</option>)}</select></label><label>Moeda<select name="moeda"><option value="">Todas</option>{opcoesMoeda}</select></label><button disabled={busy === 'extrato'}>Filtrar</button></form>{liquidacoes.length > 0 && <table><thead><tr><th>Data</th><th>Recebível</th><th>Moeda</th><th>Valor final</th></tr></thead><tbody>{liquidacoes.map(item => <tr key={item.id}><td>{new Date(item.liquidadoEm).toLocaleString('pt-BR')}</td><td>{item.recebivelId}</td><td>{item.moedaPagamento}</td><td>{dinheiro(item.valorFinal, item.moedaPagamento)}</td></tr>)}</tbody></table>}</section>
  </main>
}

export default App

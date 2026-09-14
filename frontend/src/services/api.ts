export type Cedente={id:string;nome:string;documento:string}
export type Recebivel={id:string;cedenteId:string;tipo:string;valorFace:number;dataVencimento:string;status:string}
export type Resultado={taxaBase:number;spread:number;prazoMeses:number;valorPresente:number;valorDesagio:number;moedaPagamento:string;taxaCambioUtilizada?:number;valorFinal:number}
export type Liquidacao=Resultado&{id:string;recebivelId:string;liquidadoEm:string}
const base=(import.meta.env.VITE_API_BASE_URL||'http://localhost:8080').replace(/\/$/,'')
async function call<T>(path:string,options:RequestInit={}):Promise<T>{const r=await fetch(base+path,{...options,headers:{'Content-Type':'application/json',...options.headers}});if(!r.ok)throw new Error((await r.text())||`Erro na requisição (${r.status}).`);return r.json()}
export const api={
 cedentes:()=>call<Cedente[]>('/api/cedentes'),recebiveis:()=>call<Recebivel[]>('/api/recebiveis'),
 cedente:(b:{nome:string;documento:string})=>call('/api/cedentes',{method:'POST',body:JSON.stringify(b)}),
 recebivel:(b:{cedenteId:string;tipo:string;valorFace:number;dataVencimento:string})=>call('/api/recebiveis',{method:'POST',body:JSON.stringify(b)}),
 cambio:(b:{moedaOrigem:string;moedaDestino:string;taxa:number;vigenteEm:string})=>call('/api/taxas-cambio',{method:'POST',body:JSON.stringify(b)}),
 simular:(b:Record<string,unknown>)=>call<Resultado>('/api/precificacoes/simular',{method:'POST',body:JSON.stringify(b)}),
 liquidar:(id:string,moedaPagamento:string)=>call<Liquidacao>(`/api/recebiveis/${id}/liquidacoes`,{method:'POST',headers:{'Idempotency-Key':crypto.randomUUID()},body:JSON.stringify({moedaPagamento})}),
 liquidacoes:(f:Record<string,string>)=>call<Liquidacao[]>('/api/liquidacoes?'+new URLSearchParams(Object.entries(f).filter(([,v])=>v)))
}

import { ChatCircleDots, Handshake, NotePencil } from "@phosphor-icons/react/dist/ssr";

const steps = [
  {
    number: 1,
    title: "수리 요청 등록",
    description: "고장난 곳을 사진과 함께 올려주세요. 자세할수록 더 정확한 제안을 받을 수 있어요.",
    icon: NotePencil,
  },
  {
    number: 2,
    title: "제안 받기",
    description: "주변의 수리 가능한 이웃들이 수리 방법과 견적을 제안해드려요.",
    icon: ChatCircleDots,
  },
  {
    number: 3,
    title: "채택 후 채팅·거래 진행",
    description: "마음에 드는 제안을 선택하고 채팅으로 소통하며 수리를 진행해요.",
    icon: Handshake,
  },
];

export default function HowItWorksCard() {
  return (
    <section className="reference-card how-it-works-card">
      <div className="reference-card-heading">
        <h2>이렇게 이용해요</h2>
      </div>

      <div className="how-it-works-list">
        {steps.map(({ number, title, description, icon: Icon }) => (
          <div key={number} className="how-it-works-item">
            <div className="how-it-works-number">{number}</div>

            <div className="how-it-works-icon">
              <Icon size={30} weight="duotone" />
            </div>

            <div className="how-it-works-content">
              <strong>{title}</strong>
              <p>{description}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
